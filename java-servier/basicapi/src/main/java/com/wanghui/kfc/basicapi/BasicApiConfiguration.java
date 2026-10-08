package com.wanghui.kfc.basicapi;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 配置跨品牌共用的 HTTP 客户端及默认鉴权拒绝策略。 */
@Configuration
@EnableConfigurationProperties(UpstreamProperties.class)
public class BasicApiConfiguration {
    /** 供 Spring 创建基础 API 配置。 */
    public BasicApiConfiguration() { }

    /**
     * 创建带超时及 gzip 响应解码的上游 HTTP 客户端。
     *
     * @param builder Spring 提供的客户端构建器
     * @return 上游专用客户端
     */
    @Bean
    RestClient basicRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return builder.requestFactory(factory)
                .requestInterceptor(new GzipResponseInterceptor()).build();
    }

    /**
     * 在未接入正式鉴权时拒绝请求，防止匿名误连生产接口。
     *
     * @return 默认鉴权策略
     */
    @Bean
    @ConditionalOnMissingBean(UpstreamAuthentication.class)
    UpstreamAuthentication upstreamAuthentication() {
        return (upstream, path, body, headers) -> {
            throw new IllegalStateException("Upstream authentication is not configured for " + upstream);
        };
    }
}
