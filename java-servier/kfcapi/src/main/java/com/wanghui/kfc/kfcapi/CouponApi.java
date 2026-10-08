package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import org.springframework.stereotype.Component;

/** 优惠券上游的固定接口封装。 */
@Component
public class CouponApi {
    /** 向优惠券上游固定接口发送请求的传输层。 */
    private final UpstreamGateway gateway;

    /**
     * 创建优惠券上游封装。
     * @param gateway 固定域名与路径的上游传输层
     */
    public CouponApi(UpstreamGateway gateway) { this.gateway = gateway; }

    /**
     * 查询可用优惠券，POST {@code /api/coupon/queryAvailableCouponV2}。
     * APK 证据：{@code KFC_Coupon_App/modules/02027.js} 的方法与 appcoupon 路由；参数和鉴权仍待联调。
     *
     * @param request 上游优惠券查询参数
     * @return 上游优惠券 JSON
     */
    public JsonNode availableCoupons(JsonNode request) {
        return gateway.post(KfcUpstream.COUPON, "/api/coupon/queryAvailableCouponV2", request);
    }
}
