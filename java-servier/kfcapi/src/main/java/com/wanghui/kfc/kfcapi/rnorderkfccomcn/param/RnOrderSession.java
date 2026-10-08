package com.wanghui.kfc.kfcapi.rnorderkfccomcn.param;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.basicapi.UpstreamResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpHeaders;

/** 保存一次 RN 点餐初始化后由上游签发的会话与路由状态。 */
@Data
public class RnOrderSession {
    /** 供初始化流程创建空会话。 */
    public RnOrderSession() { }

    /** 初始化响应返回并在后续请求体复用的会话标识。 */
    @ToString.Exclude
    private String sessionId;
    /** 上游响应返回的路由单元。 */
    @ToString.Exclude
    private String routeCell;
    /** 按响应顺序保存的上游 Cookie 名值。 */
    @ToString.Exclude
    private Map<String, String> cookies = new LinkedHashMap<>();

    /**
     * 吸收一次响应中的会话标识、路由单元和 Cookie。
     *
     * @param response 上游响应
     * @param initialize 是否要求从初始化响应读取 {@code data.sessionId}
     * @throws IllegalStateException 初始化响应或响应头不完整时
     */
    public void absorb(UpstreamResponse response, boolean initialize) {
        if (response == null || response.getHeaders() == null) {
            throw new IllegalStateException("RN order upstream response is missing");
        }
        if (initialize) {
            JsonNode body = response.getBody();
            JsonNode code = body == null ? null : body.get("code");
            JsonNode data = body == null ? null : body.get("data");
            JsonNode id = data == null ? null : data.get("sessionId");
            if (code == null || !code.canConvertToInt() || code.asInt() != 0
                    || id == null || !id.isTextual() || id.asText().isBlank()) {
                throw new IllegalStateException("RN order initialization did not return a session");
            }
            sessionId = id.asText();
        }
        String responseRoute = response.getHeaders().getFirst("x-yumc-route-cell");
        if (responseRoute != null && !responseRoute.isBlank()) {
            routeCell = responseRoute;
        }
        for (String setCookie : response.getHeaders().getOrDefault(HttpHeaders.SET_COOKIE, List.of())) {
            int separator = setCookie.indexOf(';');
            String pair = separator < 0 ? setCookie : setCookie.substring(0, separator);
            int equals = pair.indexOf('=');
            if (equals > 0) {
                cookies.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
            }
        }
        if (initialize) {
            requireComplete();
        }
    }

    /**
     * 生成 HTTP {@code Cookie} 请求头，不输出属性和过期时间。
     *
     * @return 当前会话 Cookie 请求头
     */
    public String cookieHeader() {
        return cookies.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * 确认后续点餐请求所需的会话状态已经建立。
     *
     * @return 当前完整会话
     * @throws IllegalStateException 会话标识、路由或 Cookie 缺失时
     */
    public RnOrderSession requireComplete() {
        List<String> requiredCookies = List.of("route-cell", "sessionIdCookie", "sessionIdCookie.sig");
        if (sessionId == null || sessionId.isBlank() || routeCell == null || routeCell.isBlank()
                || cookies == null || !cookies.keySet().containsAll(requiredCookies)) {
            throw new IllegalStateException("RN order session is incomplete");
        }
        return this;
    }
}
