package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 会员卡上游的固定接口封装。 */
@Component
public class PrimeApi {
    /** 向会员卡上游固定接口发送请求的传输层。 */
    private final UpstreamGateway gateway;

    /**
     * 创建会员卡上游封装。
     * @param gateway 固定域名与路径的上游传输层
     */
    public PrimeApi(UpstreamGateway gateway) { this.gateway = gateway; }

    /**
     * 查询用户会员卡，GET {@code /api/friend/getUserCard}。
     * APK 证据：{@code KFC_App_Activity/modules/03179.js} 的方法与 appprime 路由；鉴权仍待联调。
     * token 应来自服务端会话，不允许客户端任意指定。
     *
     * @param upstreamToken 服务端保存的上游 token
     * @return 上游会员卡 JSON
     * @throws IllegalArgumentException token 为空时
     */
    public JsonNode userCard(String upstreamToken) {
        if (upstreamToken == null || upstreamToken.isBlank()) throw new IllegalArgumentException("Upstream token required");
        return gateway.get(Upstream.PRIME, "/api/friend/getUserCard", Map.of("token", upstreamToken));
    }
}
