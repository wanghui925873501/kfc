package com.wanghui.kfc.basicapi.captcha3;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 调用用户本机部署的小辉极验三代 HTTP 服务，不记录挑战和票据。 */
@Component
public class Captcha3Client {
    /** 专用 HTTP 客户端，读取超时覆盖本地在线识别耗时。 */
    private final RestClient client;
    /** 接口地址、开关和可选访问密钥。 */
    private final Captcha3Properties properties;

    /**
     * 创建与品牌上游 HTTP 客户端隔离的本地识别客户端。
     * @param builder Spring 提供的客户端构建器
     * @param properties 本地服务配置
     */
    @Autowired
    public Captcha3Client(RestClient.Builder builder, Captcha3Properties properties) {
        SimpleClientHttpRequestFactory transport = new SimpleClientHttpRequestFactory();
        transport.setConnectTimeout(Duration.ofSeconds(3));
        transport.setReadTimeout(Duration.ofSeconds(90));
        this.client = builder.requestFactory(transport).build();
        this.properties = properties;
    }

    /**
     * 创建可注入模拟 HTTP 客户端的实例，仅供隔离测试使用。
     * @param client 模拟或受控 HTTP 客户端
     * @param properties 本地服务配置
     */
    public Captcha3Client(RestClient client, Captcha3Properties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 将当前挑战提交到显式启用的本地服务并严格校验结果。
     * @param gt 本次上游注册返回的验证 ID
     * @param challenge 本次上游注册返回的挑战标识
     * @return 含最终 challenge、validate 和 seccode 的结果
     * @throws IllegalStateException 服务未配置、超时或未返回成功结果时
     */
    public Captcha3Result solve(String gt, String challenge) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("本地图形验证服务尚未启用");
        }
        if (StrUtil.hasBlank(gt, challenge)) {
            throw new IllegalArgumentException("验证注册字段不完整");
        }
        URI target = endpoint();
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("gt", gt);
        form.add("challenge", challenge);
        try {
            JsonNode response = client.post().uri(target)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> {
                        if (StrUtil.isNotBlank(properties.getApiKey())) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    }).body(form).retrieve().body(JsonNode.class);
            JsonNode result = response == null ? null : response.path("data").path("code");
            if (result == null || !result.isObject()) result = response;
            if (result == null || !"success".equals(result.path("result").asText())
                    || StrUtil.hasBlank(result.path("challenge").asText(),
                            result.path("validate").asText())) {
                throw new IllegalStateException("本地图形验证服务未返回有效成功结果");
            }
            Captcha3Result value = new Captcha3Result();
            value.setType(result.path("type").asText(""));
            value.setChallenge(result.path("challenge").asText());
            value.setValidate(result.path("validate").asText());
            String seccode = result.path("seccode").asText("");
            value.setSeccode(StrUtil.isNotBlank(seccode)
                    ? seccode : value.getValidate() + "|jordan");
            return value;
        } catch (RestClientException e) {
            throw new IllegalStateException("本地图形验证服务请求失败或超时", e);
        }
    }

    private URI endpoint() {
        URI target = URI.create(properties.getUrl());
        if (target.getHost() == null || target.getUserInfo() != null || target.getQuery() != null
                || target.getFragment() != null || !"/captcha3".equals(target.getPath())
                || !("http".equals(target.getScheme()) || "https".equals(target.getScheme()))) {
            throw new IllegalStateException("图形验证服务 URL 必须是固定的 /captcha3 HTTP 地址");
        }
        String host = target.getHost();
        if (!"127.0.0.1".equals(host) && !"localhost".equalsIgnoreCase(host)
                && StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("局域网图形验证服务必须配置 API Key");
        }
        return target;
    }
}
