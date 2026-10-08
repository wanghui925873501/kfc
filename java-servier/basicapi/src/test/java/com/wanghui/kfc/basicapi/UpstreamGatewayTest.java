package com.wanghui.kfc.basicapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.web.client.RestClient;

class UpstreamGatewayTest {
    /** 构造测试请求 JSON 节点的 Jackson 工具。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void disabledByDefault() {
        var props = new UpstreamProperties();
        var gateway = new UpstreamGateway(RestClient.create(), props,
                (upstream, path, body, headers) -> { });
        assertThatThrownBy(() -> gateway.post(TestUpstream.ORDERING,
                "/api/v2/menu/list", mapper.createObjectNode()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("disabled");
    }

    @Test
    void rejectsUntrustedPathsAndBaseUrls() {
        var props = enabledProperties();
        props.getUrls().put(TestUpstream.ORDERING.configurationKey(),
                URI.create("https://order.example.test"));
        var gateway = new UpstreamGateway(RestClient.create(), props,
                (upstream, path, body, headers) -> { });
        assertThatThrownBy(() -> gateway.post(TestUpstream.ORDERING,
                "//evil.example/path", mapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
        props.getUrls().put(TestUpstream.ORDERING.configurationKey(),
                URI.create("http://order.example.test"));
        assertThatThrownBy(() -> gateway.post(TestUpstream.ORDERING,
                "/api/v2/menu/list", mapper.createObjectNode()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid HTTPS");
    }

    @Test
    void bindsBrandSwitchAndAddressOverride() {
        var source = new MapConfigurationPropertySource(Map.of(
                "basic.upstream.enabled.kfc", "true",
                "basic.upstream.urls.test-ordering", "https://override.example.test"));
        var properties = new Binder(source).bind(
                "basic.upstream", Bindable.of(UpstreamProperties.class)).get();
        assertThat(properties.isEnabled(TestUpstream.ORDERING)).isTrue();
        assertThat(properties.url(TestUpstream.ORDERING))
                .isEqualTo(URI.create("https://override.example.test"));
    }

    @Test
    void usesBrandOwnedDefaultDomain() {
        assertThat(new UpstreamProperties().url(TestUpstream.ORDERING))
                .isEqualTo(URI.create("https://default.example.test"));
    }

    private static UpstreamProperties enabledProperties() {
        UpstreamProperties properties = new UpstreamProperties();
        properties.getEnabled().put(Brand.KFC, true);
        return properties;
    }

    /** 为基础传输测试提供不依赖任何品牌模块的上游定义。 */
    private enum TestUpstream implements Upstream {
        /** 测试用点餐服务。 */
        ORDERING;

        /** {@inheritDoc} */
        @Override
        public Brand brand() {
            return Brand.KFC;
        }

        /** {@inheritDoc} */
        @Override
        public String configurationKey() {
            return "test-ordering";
        }

        /** {@inheritDoc} */
        @Override
        public URI defaultUrl() {
            return URI.create("https://default.example.test");
        }
    }
}
