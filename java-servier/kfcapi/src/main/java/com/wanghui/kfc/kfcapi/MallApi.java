package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 商城上游的固定接口封装。 */
@Component
public class MallApi {
    /** 向商城上游固定接口发送请求的传输层。 */
    private final UpstreamGateway gateway;

    /**
     * 创建商城上游封装。
     * @param gateway 固定域名与路径的上游传输层
     */
    public MallApi(UpstreamGateway gateway) { this.gateway = gateway; }

    /**
     * 按活动编号查询商品，GET {@code /api/kmall/getProdByActiId}。
     * APK 证据：{@code kfc-ordering-delivery/modules/02625.js} 的方法与 appmall 路由；鉴权仍待联调。
     *
     * @param activityId 活动编号
     * @return 上游活动商品 JSON
     * @throws IllegalArgumentException 活动编号格式无效时
     */
    public JsonNode productByActivityId(String activityId) {
        if (activityId == null || !activityId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid activityId");
        }
        return gateway.get(Upstream.MALL, "/api/kmall/getProdByActiId", Map.of("activityId", activityId));
    }
}
