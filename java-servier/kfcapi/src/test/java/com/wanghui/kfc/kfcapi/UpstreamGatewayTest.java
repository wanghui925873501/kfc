package com.wanghui.kfc.kfcapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.EnumMap;
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
        var gateway = new UpstreamGateway(RestClient.create(), props, (upstream, path, body, headers) -> {});
        assertThatThrownBy(() -> gateway.post(Upstream.ORDERING, "/api/v2/menu/list", mapper.createObjectNode()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("disabled");
    }

    @Test
    void rejectsUntrustedPathsAndBaseUrls() {
        var props = new UpstreamProperties();
        props.setEnabled(true);
        var urls = new EnumMap<Upstream, URI>(Upstream.class);
        urls.put(Upstream.ORDERING, URI.create("https://order.kfc.com.cn"));
        props.setUrls(urls);
        var gateway = new UpstreamGateway(RestClient.create(), props, (upstream, path, body, headers) -> {});
        assertThatThrownBy(() -> gateway.post(Upstream.ORDERING, "//evil.example/path", mapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
        urls.put(Upstream.ORDERING, URI.create("http://order.kfc.com.cn"));
        assertThatThrownBy(() -> gateway.post(Upstream.ORDERING, "/api/v2/menu/list", mapper.createObjectNode()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid HTTPS");
    }

    @Test
    void bindsCapturedAppLoginDomainToItsOwnUpstream() {
        var source = new MapConfigurationPropertySource(Map.of(
                "kfc.upstream.urls.app-login", "https://applogin.kfcapp.cn"));
        var properties = new Binder(source).bind("kfc.upstream", Bindable.of(UpstreamProperties.class)).get();
        org.assertj.core.api.Assertions.assertThat(properties.url(Upstream.APP_LOGIN))
                .isEqualTo(URI.create("https://applogin.kfcapp.cn"));
        assertThat(properties.url(Upstream.RN_ORDER))
                .isEqualTo(URI.create("https://rnorder.kfc.com.cn"));
    }

    @Test
    void providesCapturedRnOrderDomainByDefault() {
        assertThat(new UpstreamProperties().url(Upstream.RN_ORDER))
                .isEqualTo(URI.create("https://rnorder.kfc.com.cn"));
    }
}
