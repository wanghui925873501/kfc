package com.wanghui.kfc.server.controller.rnorder;

import com.wanghui.kfc.common.ApiResponse;
import com.wanghui.kfc.server.controller.rnorder.param.RnOrderDetailParam;
import com.wanghui.kfc.server.controller.rnorder.param.RnOrderMenuParam;
import com.wanghui.kfc.server.controller.rnorder.param.RnOrderStartParam;
import com.wanghui.kfc.server.controller.rnorder.param.RnOrderStoresParam;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderMenuVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderProductDetailVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoresVo;
import com.wanghui.kfc.server.rnorder.RnOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供给前端的 RN 点餐城市、选店、菜单和商品详情只读接口。 */
@RestController
@RequestMapping(value = "/api/v1/rn-order", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "RN 点餐接口", description = "上游会话仅保存在后端；当前阶段不包含购物车、下单和支付")
public class RnOrderController {
    /** RN 点餐业务编排服务。 */
    private final RnOrderService service;

    /**
     * 创建 RN 点餐控制器。
     *
     * @param service RN 点餐业务编排服务
     */
    public RnOrderController(RnOrderService service) {
        this.service = service;
    }

    /**
     * 初始化绑定当前手机号的短期点餐流程并返回城市选择数据。
     *
     * @param param 当前手机号和定位
     * @return 随机流程标识和城市选择数据
     */
    @PostMapping(value = "/flows", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "初始化点餐流程")
    public ApiResponse<RnOrderFlowVo> start(@Valid @RequestBody RnOrderStartParam param) {
        return ApiResponse.ok(service.start(param.getPhone(), param.getLatitude(),
                param.getLongitude(), param.getGbCityCode()));
    }

    /**
     * 查询当前定位附近门店和账号常用门店。
     *
     * @param param 当前流程、定位和可选门店
     * @return 选店页公开数据
     */
    @PostMapping(value = "/stores", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "查询可选门店")
    public ApiResponse<RnOrderStoresVo> stores(@Valid @RequestBody RnOrderStoresParam param) {
        return ApiResponse.ok(service.stores(param.getPhone(), param.getFlowId(),
                param.getLatitude(), param.getLongitude(), param.getGbCityCode(),
                param.getStoreCode()));
    }

    /**
     * 校验门店并返回该门店的菜单主体。
     *
     * @param param 当前流程、定位和已选择门店
     * @return 菜单页公开数据
     */
    @PostMapping(value = "/menu", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "查询门店菜单")
    public ApiResponse<RnOrderMenuVo> menu(@Valid @RequestBody RnOrderMenuParam param) {
        return ApiResponse.ok(service.menu(param.getPhone(), param.getFlowId(),
                param.getLatitude(), param.getLongitude(), param.getGbCityCode(),
                param.getStoreCode()));
    }

    /**
     * 返回一个菜单商品的详情和规格选择组。
     *
     * @param param 当前流程和商品链接标识
     * @return 商品详情页公开数据
     */
    @PostMapping(value = "/products/detail", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "查询商品详情")
    public ApiResponse<RnOrderProductDetailVo> detail(
            @Valid @RequestBody RnOrderDetailParam param) {
        return ApiResponse.ok(service.detail(param.getPhone(), param.getFlowId(),
                param.getLinkId()));
    }
}
