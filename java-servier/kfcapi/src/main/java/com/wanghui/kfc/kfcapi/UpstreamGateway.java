package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** 仅供内部接口封装使用的上游 HTTP 传输层。 */
@Component
public class UpstreamGateway {
    /** 上游 API 路径允许的字符范围，阻止完整 URL 和查询串透传。 */
    private static final Pattern SAFE_PATH = Pattern.compile("/[a-zA-Z0-9/_-]+");
    /** 设置了连接与读取超时的上游 HTTP 客户端。 */
    private final RestClient client;
    /** 固定上游地址与请求开关。 */
    private final UpstreamProperties properties;
    /** 服务端凭据与签名扩展点；未实现时会拒绝请求。 */
    private final UpstreamAuthentication authentication;

    /**
     * 创建上游传输层。
     *
     * @param kfcRestClient 带超时设置的 HTTP 客户端
     * @param properties 固定域名和总开关
     * @param authentication 服务端鉴权实现
     */
    public UpstreamGateway(RestClient kfcRestClient, UpstreamProperties properties, UpstreamAuthentication authentication) {
        this.client = kfcRestClient;
        this.properties = properties;
        this.authentication = authentication;
    }

    /**
     * 向固定上游路径发送 JSON POST 请求。
     *
     * @param upstream 上游服务
     * @param path 固定路径，不接受调用方提供的完整 URL
     * @param body JSON 请求体
     * @return 上游 JSON 响应
     * @throws IllegalStateException 上游未启用或鉴权未配置时
     * @throws UpstreamException 上游响应错误或连接失败时
     */
    public JsonNode post(Upstream upstream, String path, JsonNode body) {
        return post(upstream, path, body, Collections.emptyMap());
    }

    /**
     * 向固定上游路径发送 JSON POST 请求及业务客户端头。
     *
     * @param upstream 上游服务
     * @param path 固定路径，不接受调用方提供的完整 URL
     * @param body JSON 请求体
     * @param extraHeaders 由内部域名客户端构造的业务请求头
     * @return 上游 JSON 响应
     * @throws IllegalStateException 上游未启用或鉴权未配置时
     * @throws UpstreamException 上游响应错误或连接失败时
     */
    public JsonNode post(Upstream upstream, String path, JsonNode body, Map<String, String> extraHeaders) {
        URI target = target(upstream, path);
        String bodyJson = body.toString();
        try {
            return client.post().uri(target).contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (upstream == Upstream.APP_LOGIN) {
                            headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
                            headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
                        }
                        extraHeaders.forEach(headers::set);
                        authentication.apply(upstream, path, bodyJson, headers);
                    })
                    .body(bodyJson).retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new UpstreamException(upstream, response.getStatusCode().value(), "Upstream request failed");
                    }).body(JsonNode.class);
        } catch (UpstreamException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UpstreamException(upstream, 502, "Upstream connection failed");
        }
    }

    /**
     * 向固定上游路径发送带编码查询参数的 GET 请求。
     *
     * @param upstream 上游服务
     * @param path 固定路径，不接受调用方提供的完整 URL
     * @param query 由业务接口封装构造的查询参数
     * @return 上游 JSON 响应
     * @throws IllegalStateException 上游未启用或鉴权未配置时
     * @throws UpstreamException 上游响应错误或连接失败时
     */
    public JsonNode get(Upstream upstream, String path, Map<String, String> query) {
        return get(upstream, path, query, Collections.emptyMap());
    }

    /**
     * 向固定路径发送带客户端业务头和签名查询串的 GET 请求。
     * @param upstream 上游服务
     * @param path 固定路径
     * @param query 查询字段
     * @param extraHeaders 客户端业务头
     * @return 上游 JSON 响应
     * @throws IllegalStateException 上游未启用或鉴权未配置时
     * @throws UpstreamException 上游响应错误或连接失败时
     */
    public JsonNode get(Upstream upstream, String path, Map<String, String> query,
                        Map<String, String> extraHeaders) {
        var builder = UriComponentsBuilder.fromUri(target(upstream, path));
        query.forEach(builder::queryParam);
        URI target = builder.build().encode().toUri();
        String signedQuery = new TreeMap<>(query).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        try {
            return client.get().uri(target)
                    .headers(headers -> {
                        extraHeaders.forEach(headers::set);
                        authentication.apply(upstream, path, signedQuery, headers);
                    })
                    .retrieve().onStatus(status -> status.isError(), (request, response) -> {
                        throw new UpstreamException(upstream, response.getStatusCode().value(), "Upstream request failed");
                    }).body(JsonNode.class);
        } catch (UpstreamException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UpstreamException(upstream, 502, "Upstream connection failed");
        }
    }

    private URI target(Upstream upstream, String path) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("KFC upstream calls are disabled; set KFC_UPSTREAM_ENABLED=true after configuring credentials");
        }
        if (!SAFE_PATH.matcher(path).matches() || path.contains("//") || path.contains("..")) {
            throw new IllegalArgumentException("Invalid upstream path");
        }
        return properties.url(upstream).resolve(path);
    }
}
