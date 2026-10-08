package com.wanghui.kfc.kfcapi.rnorderkfccomcn.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wanghui.kfc.kfcapi.Upstream;
import com.wanghui.kfc.kfcapi.UpstreamGateway;
import com.wanghui.kfc.kfcapi.UpstreamResponse;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderContext;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderLocation;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.support.RnOrderProperties;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.vo.RnOrderInitResult;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 封装 {@code rnorder.kfc.com.cn} 已抓包确认的首批只读选店与菜单接口。 */
@Component
public class RnOrderApi {
    /** 预点餐初始化传输路径。 */
    private static final String INIT_PATH = "/preorder-portal/api/v2/init/combine/preorder";
    /** 经纬度反查城市传输路径。 */
    private static final String CITY_BY_LOCATION_PATH = "/store-portal/api/v2/city/getCityByRgeoCode";
    /** 城市列表传输路径。 */
    private static final String CITIES_PATH = "/store-portal/api/v2/city/cities";
    /** 附近门店传输路径。 */
    private static final String SEARCH_STORES_PATH = "/store-portal/api/v2/store/searchByLbs";
    /** 用户常用门店传输路径。 */
    private static final String QUERY_STORES_PATH = "/store-portal/api/v2/customer/queryStores";
    /** 门店有效性校验传输路径。 */
    private static final String VALID_STORE_PATH = "/store-portal/api/v2/store/validStore";
    /** 菜单列表传输路径。 */
    private static final String MENU_LIST_PATH = "/preorder-portal/api/v2/menu/list";
    /** 商品详情传输路径。 */
    private static final String MENU_DETAIL_PATH = "/preorder-portal/api/v3/menu/detail";
    /** 受固定域名和固定路径限制的上游传输层。 */
    private final UpstreamGateway gateway;
    /** 请求 JSON 构造器。 */
    private final ObjectMapper mapper;
    /** 与抓包协议一致的 DES 字段加密器。 */
    private final AppLoginCrypto crypto;
    /** 共用的 KFC 请求体配置。 */
    private final AppLoginProperties credentials;
    /** RN 点餐客户端固定档案。 */
    private final RnOrderProperties properties;
    /** 生成请求头 UUID 后缀的时间源。 */
    private final Clock clock;
    /** 生成请求头 UUID 前缀的随机源。 */
    private final Supplier<UUID> uuidSupplier;

    /**
     * 创建生产环境 RN 点餐客户端。
     *
     * @param gateway 固定域名和路径的上游传输层
     * @param mapper JSON 构造器
     * @param crypto 已验证 DES 加密器
     * @param credentials KFC 请求体公共配置
     * @param properties RN 点餐客户端档案
     */
    @Autowired
    public RnOrderApi(UpstreamGateway gateway, ObjectMapper mapper, AppLoginCrypto crypto,
                      AppLoginProperties credentials, RnOrderProperties properties) {
        this(gateway, mapper, crypto, credentials, properties, Clock.systemUTC(), UUID::randomUUID);
    }

    RnOrderApi(UpstreamGateway gateway, ObjectMapper mapper, AppLoginCrypto crypto,
               AppLoginProperties credentials, RnOrderProperties properties,
               Clock clock, Supplier<UUID> uuidSupplier) {
        this.gateway = gateway;
        this.mapper = mapper;
        this.crypto = crypto;
        this.credentials = credentials;
        this.properties = properties;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }

    /**
     * 初始化预点餐会话。
     *
     * <p>调用 {@code POST /preorder-portal/api/v2/init/combine/preorder}。Reqable 证据：
     * {@code 抓包文件/选店/01-[6736]-init-combine-preorder}。请求中的四个经纬度字段按
     * {@code encodeList} 使用 DES 加密；响应会话 Cookie 和路由头只保存在返回的内部对象中。</p>
     *
     * @param context 已登录用户和真实安装上下文
     * @param location 当前定位
     * @return 初始化响应及后续请求必须复用的会话
     */
    public RnOrderInitResult initializePreorder(RnOrderContext context, RnOrderLocation location) {
        require(context, location);
        ObjectNode body = mapper.createObjectNode();
        appendClientProfile(body, false);
        body.put("deviceId", context.getDeviceId());
        body.put("clientVersion", properties.getClientVersion());
        body.put("fversion", properties.getFversion());
        body.put("versionNum", properties.getVersionNum());
        ObjectNode requestBody = body.putObject("body");
        ObjectNode geoLocation = requestBody.putObject("geoLocation");
        String encryptedLongitude = crypto.encrypt(location.getLongitude());
        String encryptedLatitude = crypto.encrypt(location.getLatitude());
        geoLocation.put("lng", encryptedLongitude);
        geoLocation.put("lat", encryptedLatitude);
        body.put("ticket", context.getTicket());
        body.put("userLongitude", encryptedLongitude);
        body.put("userLatitude", encryptedLatitude);
        body.putArray("encodeList").add("body.geoLocation.lng").add("body.geoLocation.lat")
                .add("userLatitude").add("userLongitude");
        appendCustomerMarker(body);
        UpstreamResponse response = gateway.postForResponse(Upstream.RN_ORDER, INIT_PATH, body,
                headers(context, null));
        requireJsonObject(response);
        RnOrderSession session = new RnOrderSession();
        session.absorb(response, true);
        RnOrderInitResult result = new RnOrderInitResult();
        result.setResponse(response.getBody());
        result.setSession(session);
        return result;
    }

    /**
     * 按经纬度查询城市。
     *
     * <p>调用 {@code POST /store-portal/api/v2/city/getCityByRgeoCode}。Reqable 证据：
     * {@code 抓包文件/选店/03-[6742]-city-by-regeo-code}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param location 当前定位
     * @return 上游城市 JSON
     */
    public JsonNode getCityByLocation(RnOrderContext context, RnOrderSession session,
                                      RnOrderLocation location) {
        require(context, session, location);
        ObjectNode body = storeBody(context, session);
        body.put("mylat", crypto.encrypt(location.getLatitude()));
        body.put("mylng", crypto.encrypt(location.getLongitude()));
        body.putArray("encodeList").add("mylat").add("mylng");
        appendCustomerMarker(body);
        return post(context, session, CITY_BY_LOCATION_PATH, body);
    }

    /**
     * 查询可选城市列表。
     *
     * <p>调用 {@code POST /store-portal/api/v2/city/cities}。Reqable 证据：
     * {@code 抓包文件/选店/07-[6751]-cities}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @return 上游城市列表 JSON
     */
    public JsonNode cities(RnOrderContext context, RnOrderSession session) {
        require(context, session);
        ObjectNode body = storeBody(context, session);
        body.putArray("encodeList");
        appendCustomerMarker(body);
        return post(context, session, CITIES_PATH, body);
    }

    /**
     * 查询定位附近的门店。
     *
     * <p>调用 {@code POST /store-portal/api/v2/store/searchByLbs}。Reqable 证据：
     * {@code 抓包文件/选店/04-[6743]-search-by-lbs-first} 和
     * {@code 08-[6753]-search-by-lbs-loaded}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param location 当前定位和可选国标城市编码
     * @param storeCode 可选的当前门店编码，传空字符串表示没有当前门店
     * @return 上游附近门店 JSON
     */
    public JsonNode searchStoresByLbs(RnOrderContext context, RnOrderSession session,
                                      RnOrderLocation location, String storeCode) {
        require(context, session, location);
        ObjectNode body = storeBody(context, session);
        String encryptedLatitude = crypto.encrypt(location.getLatitude());
        String encryptedLongitude = crypto.encrypt(location.getLongitude());
        body.put("mylat", encryptedLatitude);
        body.put("mylng", encryptedLongitude);
        body.put("storeCode", valueOrEmpty(storeCode));
        body.put("callScene", "storeScene");
        body.put("screenActivityId", "");
        body.put("activitySource", "");
        body.put("mylatPhone", encryptedLatitude);
        body.put("mylngPhone", encryptedLongitude);
        body.put("gbCityCode", valueOrEmpty(location.getGbCityCode()));
        body.putArray("encodeList").add("mylat").add("mylng")
                .add("mylatPhone").add("mylngPhone");
        appendCustomerMarker(body);
        return post(context, session, SEARCH_STORES_PATH, body);
    }

    /**
     * 查询当前账号收藏或常用的门店。
     *
     * <p>调用 {@code POST /store-portal/api/v2/customer/queryStores}。Reqable 证据：
     * {@code 抓包文件/选店/05-[6745]-query-stores}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param location 当前定位
     * @return 上游用户门店 JSON
     */
    public JsonNode queryCustomerStores(RnOrderContext context, RnOrderSession session,
                                        RnOrderLocation location) {
        require(context, session, location);
        ObjectNode body = storeBody(context, session);
        body.put("mylatPhone", crypto.encrypt(location.getLatitude()));
        body.put("mylngPhone", crypto.encrypt(location.getLongitude()));
        body.putArray("encodeList").add("mylatPhone").add("mylngPhone");
        appendCustomerMarker(body);
        return post(context, session, QUERY_STORES_PATH, body);
    }

    /**
     * 校验一个门店并取得点餐所需的完整门店对象。
     *
     * <p>调用 {@code POST /store-portal/api/v2/store/validStore}。Reqable 证据：
     * {@code 抓包文件/菜单首页/01-[6806]-valid-store}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param location 当前定位
     * @param storeCode 必填门店编码
     * @return 上游门店校验 JSON
     */
    public JsonNode validStore(RnOrderContext context, RnOrderSession session,
                               RnOrderLocation location, String storeCode) {
        require(context, session, location);
        requireText(storeCode, "storeCode");
        ObjectNode body = storeBody(context, session);
        body.put("storeCode", storeCode);
        body.put("mylat", crypto.encrypt(location.getLatitude()));
        body.put("mylng", crypto.encrypt(location.getLongitude()));
        body.putArray("encodeList").add("mylat").add("mylng");
        appendCustomerMarker(body);
        return post(context, session, VALID_STORE_PATH, body);
    }

    /**
     * 查询已校验门店的预点餐菜单。
     *
     * <p>调用 {@code POST /preorder-portal/api/v2/menu/list}。Reqable 证据：
     * {@code 抓包文件/菜单首页/03-[6811]-menu-list}。{@code store} 必须直接来自同一会话的
     * {@link #validStore(RnOrderContext, RnOrderSession, RnOrderLocation, String)} 响应，调用方
     * 不应拼装或覆盖其中字段。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param store 同一会话校验返回的完整门店对象
     * @return 上游菜单 JSON
     */
    public JsonNode menuList(RnOrderContext context, RnOrderSession session, JsonNode store) {
        require(context, session);
        if (store == null || !store.isObject()) {
            throw new IllegalArgumentException("store must be an upstream JSON object");
        }
        ObjectNode body = preorderBody(context, session);
        body.set("store", store.deepCopy());
        body.put("env", properties.getEnv());
        body.putArray("encodeList");
        appendCustomerMarker(body);
        return post(context, session, MENU_LIST_PATH, body);
    }

    /**
     * 查询一个菜单商品的详情。
     *
     * <p>调用 {@code POST /preorder-portal/api/v3/menu/detail}。Reqable 证据：
     * {@code 抓包文件/商品详情/01-[7153]-menu-detail}。</p>
     *
     * @param context 已登录用户和安装上下文
     * @param session 已初始化的 RN 点餐会话
     * @param linkId 菜单返回的商品链接标识
     * @param descNeed 是否请求详情描述
     * @return 上游商品详情 JSON
     */
    public JsonNode menuDetail(RnOrderContext context, RnOrderSession session,
                               String linkId, boolean descNeed) {
        require(context, session);
        requireText(linkId, "linkId");
        ObjectNode body = preorderBody(context, session);
        body.put("descNeed", descNeed);
        body.put("linkId", linkId);
        body.put("env", properties.getEnv());
        body.putArray("encodeList");
        appendCustomerMarker(body);
        return post(context, session, MENU_DETAIL_PATH, body);
    }

    private ObjectNode storeBody(RnOrderContext context, RnOrderSession session) {
        ObjectNode body = mapper.createObjectNode();
        appendClientProfile(body, true);
        appendSessionProfile(body, context, session);
        body.put("env", properties.getEnv());
        body.put("fversion", properties.getFversion());
        return body;
    }

    private ObjectNode preorderBody(RnOrderContext context, RnOrderSession session) {
        ObjectNode body = mapper.createObjectNode();
        appendClientProfile(body, false);
        appendSessionProfile(body, context, session);
        body.put("fversion", properties.getFversion());
        body.put("versionNum", properties.getVersionNum());
        return body;
    }

    private void appendClientProfile(ObjectNode body, boolean portalSource) {
        RnOrderProperties profile = properties.requireComplete();
        body.put("portalType", profile.getPortalType());
        if (portalSource) {
            body.put("portalSource", "");
        }
        body.put("channelName", profile.getChannelName());
        body.put("channelId", profile.getChannelId());
        body.put("brand", profile.getBrand());
        body.put("business", profile.getBusiness());
    }

    private void appendSessionProfile(ObjectNode body, RnOrderContext context,
                                      RnOrderSession session) {
        body.put("sessionId", session.getSessionId());
        body.put("deviceId", context.getDeviceId());
        body.put("clientVersion", properties.getClientVersion());
        body.put("ticket", context.getTicket());
    }

    private void appendCustomerMarker(ObjectNode body) {
        body.put("isFromCustomerClient", true);
        body.put("secretKey", credentials.requireRequestSecretKey());
    }

    private JsonNode post(RnOrderContext context, RnOrderSession session,
                          String path, ObjectNode body) {
        UpstreamResponse response = gateway.postForResponse(Upstream.RN_ORDER, path, body,
                headers(context, session));
        requireJsonObject(response);
        session.absorb(response, false);
        return response.getBody();
    }

    private Map<String, String> headers(RnOrderContext context, RnOrderSession session) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HttpHeaders.ACCEPT, "*/*");
        headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
        headers.put("rn_bundle", properties.getRnBundle());
        headers.put(HttpHeaders.USER_AGENT, context.getUserAgent());
        headers.put("uuid", uuidSupplier.get() + Long.toString(clock.millis()));
        headers.put("x-yumc-client-channel", "app");
        headers.put("x-yumc-client-citycode", context.getCityCode());
        headers.put("x-yumc-client-deviceid", context.getDeviceId());
        headers.put("x-yumc-client-usercode", context.getUserCode());
        if (session != null) {
            headers.put("x-yumc-route-cell", session.getRouteCell());
            headers.put(HttpHeaders.COOKIE, session.cookieHeader());
        }
        return headers;
    }

    private void require(RnOrderContext context, RnOrderLocation location) {
        if (context == null || location == null) {
            throw new IllegalArgumentException("RN order context and location are required");
        }
        context.requireComplete();
        location.requireValid();
        properties.requireComplete();
    }

    private void require(RnOrderContext context, RnOrderSession session) {
        if (context == null || session == null) {
            throw new IllegalArgumentException("RN order context and session are required");
        }
        context.requireComplete();
        session.requireComplete();
        properties.requireComplete();
    }

    private void require(RnOrderContext context, RnOrderSession session,
                         RnOrderLocation location) {
        require(context, session);
        if (location == null) {
            throw new IllegalArgumentException("RN order location is required");
        }
        location.requireValid();
    }

    private void requireJsonObject(UpstreamResponse response) {
        if (response == null || response.getBody() == null || !response.getBody().isObject()) {
            throw new IllegalStateException("RN order upstream returned an invalid JSON response");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
