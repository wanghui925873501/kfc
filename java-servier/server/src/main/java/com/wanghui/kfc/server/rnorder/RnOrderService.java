package com.wanghui.kfc.server.rnorder;

import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.api.RnOrderApi;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderContext;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderLocation;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.vo.RnOrderInitResult;
import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderMenuVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderProductDetailVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoresVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 编排已登录用户的 RN 点餐初始化、选店、菜单和商品详情只读流程。 */
@Service
public class RnOrderService {
    /** RN 点餐上游客户端。 */
    private final RnOrderApi api;
    /** 当前已登录请求上下文。 */
    private final KfcRequestContextHolder requestContextHolder;
    /** 加密的短期流程存储。 */
    private final RnOrderFlowStore flowStore;
    /** 上游 JSON 白名单映射器。 */
    private final RnOrderViewMapper viewMapper;

    /**
     * 创建 RN 点餐业务服务。
     *
     * @param api RN 点餐上游客户端
     * @param requestContextHolder 当前已登录请求上下文
     * @param flowStore 加密的短期流程存储
     * @param viewMapper 上游 JSON 白名单映射器
     */
    public RnOrderService(RnOrderApi api, KfcRequestContextHolder requestContextHolder,
                          RnOrderFlowStore flowStore, RnOrderViewMapper viewMapper) {
        this.api = api;
        this.requestContextHolder = requestContextHolder;
        this.flowStore = flowStore;
        this.viewMapper = viewMapper;
    }

    /**
     * 初始化一次绑定当前手机号的 RN 点餐浏览流程。
     *
     * @param phone 当前授权手机号
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @param gbCityCode 可选国标城市编码
     * @return 随机流程标识和城市页数据
     */
    public RnOrderFlowVo start(String phone, String latitude, String longitude,
                               String gbCityCode) {
        KfcRequestContext request = request(phone);
        RnOrderContext context = context(request);
        RnOrderLocation location = location(latitude, longitude, gbCityCode);
        RnOrderInitResult initialized = api.initializePreorder(context, location);
        requireSuccess(initialized == null ? null : initialized.getResponse(), "初始化");
        if (initialized == null || initialized.getSession() == null) throw rejected("初始化");
        JsonNode locatedCity = api.getCityByLocation(context, initialized.getSession(), location);
        requireSuccess(locatedCity, "城市定位");
        JsonNode cities = api.cities(context, initialized.getSession());
        requireSuccess(cities, "城市列表");
        RnOrderFlowState state = new RnOrderFlowState();
        state.setPhoneHash(SecureUtil.sha256(phone));
        state.setSession(initialized.getSession());
        String flowId = flowStore.issue(state);
        RnOrderFlowVo result = viewMapper.flow(locatedCity, cities);
        result.setFlowId(flowId);
        result.setExpiresInSeconds(flowStore.expiresInSeconds());
        return result;
    }

    /**
     * 查询当前定位附近门店和当前账号常用门店。
     *
     * @param phone 当前授权手机号
     * @param flowId 当前手机号的点餐流程标识
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @param gbCityCode 可选国标城市编码
     * @param storeCode 可选当前门店编码
     * @return 选店页公开数据
     */
    public RnOrderStoresVo stores(String phone, String flowId, String latitude,
                                  String longitude, String gbCityCode, String storeCode) {
        KfcRequestContext request = request(phone);
        RnOrderFlowState state = flow(flowId, phone);
        RnOrderContext context = context(request);
        RnOrderLocation location = location(latitude, longitude, gbCityCode);
        JsonNode nearby = api.searchStoresByLbs(context, state.getSession(), location, storeCode);
        requireSuccess(nearby, "附近门店");
        JsonNode customer = api.queryCustomerStores(context, state.getSession(), location);
        requireSuccess(customer, "常用门店");
        flowStore.save(flowId, state);
        return viewMapper.stores(nearby, customer);
    }

    /**
     * 校验用户选择的门店并查询该门店菜单。
     *
     * @param phone 当前授权手机号
     * @param flowId 当前手机号的点餐流程标识
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @param gbCityCode 可选国标城市编码
     * @param storeCode 用户选择的门店编码
     * @return 菜单页公开数据
     */
    public RnOrderMenuVo menu(String phone, String flowId, String latitude, String longitude,
                              String gbCityCode, String storeCode) {
        KfcRequestContext request = request(phone);
        RnOrderFlowState state = flow(flowId, phone);
        RnOrderContext context = context(request);
        RnOrderLocation location = location(latitude, longitude, gbCityCode);
        JsonNode validated = api.validStore(context, state.getSession(), location, storeCode);
        requireSuccess(validated, "门店校验");
        JsonNode store = validated.path("data").path("store");
        if (!store.isObject()) throw rejected("门店校验");
        JsonNode menu = api.menuList(context, state.getSession(), store);
        requireSuccess(menu, "菜单");
        flowStore.save(flowId, state);
        return viewMapper.menu(store, menu);
    }

    /**
     * 查询菜单商品的只读详情和规格选项。
     *
     * @param phone 当前授权手机号
     * @param flowId 当前手机号的点餐流程标识
     * @param linkId 菜单返回的商品链接标识
     * @return 商品详情页公开数据
     */
    public RnOrderProductDetailVo detail(String phone, String flowId, String linkId) {
        KfcRequestContext request = request(phone);
        RnOrderFlowState state = flow(flowId, phone);
        JsonNode detail = api.menuDetail(context(request), state.getSession(), linkId, true);
        requireSuccess(detail, "商品详情");
        flowStore.save(flowId, state);
        return viewMapper.detail(detail);
    }

    private KfcRequestContext request(String phone) {
        KfcRequestContext request = requestContextHolder.require();
        if (!phone.equals(request.getPhone())) {
            throw new RnOrderException("RN_ORDER_CONTEXT_MISMATCH", "当前登录上下文不匹配",
                    HttpStatus.CONFLICT);
        }
        return request;
    }

    private RnOrderFlowState flow(String flowId, String phone) {
        return flowStore.resolve(flowId, SecureUtil.sha256(phone)).orElseThrow(() ->
                new RnOrderException("RN_ORDER_FLOW_EXPIRED", "点餐流程已过期，请重新进入",
                        HttpStatus.CONFLICT));
    }

    private RnOrderContext context(KfcRequestContext request) {
        if (request.getInstallation() == null || request.getKfcUser() == null
                || request.getPhoneInstallation() == null) {
            throw new RnOrderException("RN_ORDER_CONTEXT_INCOMPLETE", "当前登录上下文不完整",
                    HttpStatus.CONFLICT);
        }
        RnOrderContext result = new RnOrderContext();
        result.setDeviceId(request.getInstallation().getDeviceId());
        result.setUserCode(request.getKfcUser().getUserCode());
        result.setTicket(request.getUpstreamToken());
        result.setCityCode(request.getPhoneInstallation().getCityCode());
        result.setUserAgent(request.getPhoneInstallation().getUserAgent());
        try {
            return result.requireComplete();
        } catch (IllegalArgumentException e) {
            throw new RnOrderException("RN_ORDER_CONTEXT_INCOMPLETE", "当前登录上下文不完整",
                    HttpStatus.CONFLICT);
        }
    }

    private RnOrderLocation location(String latitude, String longitude, String gbCityCode) {
        RnOrderLocation result = new RnOrderLocation();
        result.setLatitude(latitude);
        result.setLongitude(longitude);
        result.setGbCityCode(gbCityCode);
        return result.requireValid();
    }

    private void requireSuccess(JsonNode response, String stage) {
        JsonNode code = response == null ? null : response.get("code");
        if (code == null || !code.canConvertToInt() || code.asInt() != 0
                || response.get("data") == null) {
            throw rejected(stage);
        }
    }

    private RnOrderException rejected(String stage) {
        return new RnOrderException("RN_ORDER_REJECTED", stage + "未成功",
                HttpStatus.BAD_GATEWAY);
    }
}
