package com.wanghui.kfc.alipay.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.alipay.config.KfcAliPayProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 通过 Chromium CDP 创建支付宝页面，仅负责可视化人工交接，不执行点击或密码输入。
 */
@Component
public class CdpBrowserHandoffClient {

    /** 支付与 CDP 配置。 */
    private final KfcAliPayProperties properties;

    /** Chromium 调试 HTTP 客户端。 */
    private final HttpClient httpClient;

    /** JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建 CDP 交接客户端。
     *
     * @param properties 配置
     * @param objectMapper JSON 解析器
     */
    public CdpBrowserHandoffClient(KfcAliPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * 在预先启动的 Chromium 中打开支付宝官方链接。
     *
     * @param paymentUrl 一次性支付宝 H5 链接
     * @return 新页面 target 标识
     * @throws IOException CDP 调用失败
     * @throws InterruptedException 等待 CDP 响应时线程中断
     */
    public String open(String paymentUrl) throws IOException, InterruptedException {
        KfcAliPayProperties.Cdp cdp = properties.cdp();
        if (cdp == null || !cdp.enabled()) {
            throw new IllegalStateException("CDP handoff is disabled");
        }
        URI paymentUri = URI.create(paymentUrl);
        if (!"https".equalsIgnoreCase(paymentUri.getScheme())
                || cdp.allowedHosts() == null
                || cdp.allowedHosts().stream().noneMatch(paymentUri.getHost()::equalsIgnoreCase)) {
            throw new IllegalArgumentException("paymentUrl must use an allowed Alipay HTTPS host");
        }

        String endpoint = "http://" + cdp.debugAddress() + "/json/new?"
                + URLEncoder.encode(paymentUrl, StandardCharsets.UTF_8);
        HttpResponse<String> response = send(endpoint, "PUT");
        if (response.statusCode() == 404 || response.statusCode() == 405) {
            response = send(endpoint, "GET");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("CDP create target failed, HTTP " + response.statusCode());
        }
        JsonNode body = objectMapper.readTree(response.body());
        String targetId = body.path("id").asText();
        if (targetId.isBlank()) {
            throw new IOException("CDP response did not contain target id");
        }
        return targetId;
    }

    /** 发送兼容新旧 Chromium 的调试请求。 */
    private HttpResponse<String> send(String endpoint, String method)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10));
        HttpRequest request = "PUT".equals(method)
                ? builder.PUT(HttpRequest.BodyPublishers.noBody()).build()
                : builder.GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
