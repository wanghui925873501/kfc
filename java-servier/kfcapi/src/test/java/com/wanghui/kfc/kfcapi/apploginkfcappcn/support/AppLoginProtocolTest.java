package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.kfcapi.KfcApiConfiguration;
import com.wanghui.kfc.kfcapi.GzipResponseInterceptor;
import com.wanghui.kfc.kfcapi.Upstream;
import com.wanghui.kfc.kfcapi.UpstreamAuthentication;
import com.wanghui.kfc.kfcapi.UpstreamGateway;
import com.wanghui.kfc.kfcapi.UpstreamProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AppLoginProtocolTest {
    @Test
    void configuresOnlyTheDomainSpecificAuthenticationBean() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            context.register(AppLoginProperties.class, AppLoginAuthentication.class, KfcApiConfiguration.class);
            context.refresh();
            assertThat(context.getBeansOfType(UpstreamAuthentication.class)).hasSize(1)
                    .containsValue(context.getBean(AppLoginAuthentication.class));
        }
    }

    @Test
    void encryptsWithObservedDesMode() {
        AppLoginProperties properties = new AppLoginProperties();
        properties.setDesKey("12345678");
        assertThat(new AppLoginCrypto(properties).encrypt("test-value"))
                .isEqualTo("604xISIBN/T1giD2D4t9iQ==");
    }

    @Test
    void signsExactlyTheBodySentOverHttp() throws IOException {
        assertThat(AppLoginSignature.sign("key", "secret", "123", "/user/sendSmsCode",
                "{\"phone\":\"cipher\"}"))
                .isEqualTo("6cd82940a299fa6806d896b09dd809ac");
        assertThat(AppLoginSignature.sign("key", "secret", "123", "/user/loginBySmsCode",
                "{\"phone\":\"cipher\"}"))
                .isEqualTo("69ae948d3c4be590e5b1bed9166ef9f5");

        AppLoginProperties properties = new AppLoginProperties();
        properties.setClientKey("key");
        properties.setClientSecret("secret");
        var authentication = new AppLoginAuthentication(properties,
                Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC));
        var upstreamProperties = new UpstreamProperties();
        upstreamProperties.setEnabled(true);
        Map<Upstream, URI> urls = new EnumMap<>(Upstream.class);
        urls.put(Upstream.APP_LOGIN, URI.create("https://applogin.kfcapp.cn"));
        upstreamProperties.setUrls(urls);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        builder.requestInterceptor(new GzipResponseInterceptor());
        var gateway = new UpstreamGateway(builder.build(), upstreamProperties, authentication);

        server.expect(once(), requestTo("https://applogin.kfcapp.cn/api/user/sendSmsCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"phone\":\"cipher\"}"))
                .andExpect(header("Content-Type", "application/json; charset=utf-8"))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().getCharset())
                        .isEqualTo(StandardCharsets.UTF_8))
                .andExpect(header("Accept-Encoding", "gzip"))
                .andExpect(header("kbck", "key"))
                .andExpect(header("kbcts", "123"))
                .andExpect(header("kbsv", "6cd82940a299fa6806d896b09dd809ac"))
                .andExpect(header("rcsav", "test-version"))
                .andRespond(withSuccess(gzip("{\"errCode\":0}"), MediaType.APPLICATION_JSON)
                        .header("Content-Encoding", "gzip"));

        server.expect(once(), requestTo("https://applogin.kfcapp.cn/api/user/loginBySmsCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"phone\":\"cipher\"}"))
                .andExpect(header("kbsv", "69ae948d3c4be590e5b1bed9166ef9f5"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(gateway.post(Upstream.APP_LOGIN, "/api/user/sendSmsCode",
                new ObjectMapper().createObjectNode().put("phone", "cipher"),
                Map.of("rcsav", "test-version")).get("errCode").asInt()).isZero();
        gateway.post(Upstream.APP_LOGIN, "/api/user/loginBySmsCode",
                new ObjectMapper().createObjectNode().put("phone", "cipher"), Map.of());
        server.verify();
    }

    @Test
    void signsCaptchaGetAndVerifiedPostUsingApkShortPaths() {
        assertThat(AppLoginSignature.signGet("key", "secret", "123", "/svc/startCaptcha",
                "ct=native&rt=1&type=MOBILE")).isEqualTo("b0c72f984c45325228313b80f992e04c");
        AppLoginProperties properties = new AppLoginProperties();
        properties.setClientKey("key");
        properties.setClientSecret("secret");
        var authentication = new AppLoginAuthentication(properties,
                Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC));
        var upstreamProperties = new UpstreamProperties();
        upstreamProperties.setEnabled(true);
        Map<Upstream, URI> urls = new EnumMap<>(Upstream.class);
        urls.put(Upstream.APP_LOGIN, URI.create("https://applogin.kfcapp.cn"));
        upstreamProperties.setUrls(urls);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UpstreamGateway gateway = new UpstreamGateway(builder.build(), upstreamProperties, authentication);
        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/api/svc/startCaptcha");
            assertThat(request.getURI().getQuery()).contains("rt=1", "type=MOBILE", "ct=native");
        }).andExpect(method(HttpMethod.GET))
                .andExpect(header("kbsv", "b0c72f984c45325228313b80f992e04c"))
                .andExpect(header("rcsav", "test-version"))
                .andRespond(withSuccess("{\"errCode\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://applogin.kfcapp.cn/api/svc/to/user/sendSmsCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("{\"phone\":\"cipher\"}"))
                .andExpect(header("kbsv", "459b609ef26013273b6686e9b6ab43d8"))
                .andRespond(withSuccess("{\"errCode\":0}", MediaType.APPLICATION_JSON));
        gateway.get(Upstream.APP_LOGIN, "/api/svc/startCaptcha",
                Map.of("rt", "1", "type", "MOBILE", "ct", "native"),
                Map.of("rcsav", "test-version"));
        gateway.post(Upstream.APP_LOGIN, "/api/svc/to/user/sendSmsCode",
                new ObjectMapper().createObjectNode().put("phone", "cipher"));
        server.verify();
    }

    private static byte[] gzip(String json) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}
