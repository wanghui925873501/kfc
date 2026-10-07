package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** 点餐上游的固定接口封装。 */
@Component
public class OrderingApi {
    /** 向点餐上游固定接口发送请求的传输层。 */
    private final UpstreamGateway gateway;

    /**
     * 创建点餐上游封装。
     * @param gateway 固定域名与路径的上游传输层
     */
    public OrderingApi(UpstreamGateway gateway) { this.gateway = gateway; }

    /**
     * 查询菜单，POST {@code /api/v2/menu/list}。
     * APK 证据：{@code kfc-ordering-delivery/modules/01517.js}；目标 API 域名、参数和鉴权仍待联调。
     *
     * @param request 上游菜单查询参数
     * @return 上游菜单 JSON
     */
    public JsonNode menuList(JsonNode request) {
        return gateway.post(Upstream.ORDERING, "/api/v2/menu/list", request);
    }
}
