package com.wanghui.kfc.kfcapi.rnorderkfccomcn.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.kfcapi.GzipResponseInterceptor;
import com.wanghui.kfc.kfcapi.KfcRequestSignature;
import com.wanghui.kfc.kfcapi.UpstreamGateway;
import com.wanghui.kfc.kfcapi.UpstreamProperties;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.api.RnOrderApi;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderContext;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderLocation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RnOrderProtocolTest {
    /** 测试 JSON 解析器。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void initializesSessionAndReusesCookiesForUnsignedStoreSearch() throws IOException {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(
                        "https://rnorder.kfc.com.cn/preorder-portal/api/v2/init/combine/preorder"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("kbck", "key"))
                .andExpect(header("kbcts", "123"))
                .andExpect(header("x-yumc-client-usercode", "user-code"))
                .andExpect(request -> {
                    JsonNode body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    assertThat(body.get("body").get("geoLocation").get("lat").asText())
                            .isNotEqualTo("31.2304");
                    assertThat(body.get("ticket").asText()).isEqualTo("upstream-ticket");
                    String json = body.toString();
                    assertThat(request.getHeaders().getFirst("kbsv")).isEqualTo(
                            KfcRequestSignature.signPost("key", "secret", "123",
                                    "/api/v2/init/combine/preorder", json));
                })
                .andRespond(withSuccess("{\"code\":0,\"data\":{\"sessionId\":\"session-1\"}}",
                        MediaType.APPLICATION_JSON)
                        .header("x-yumc-route-cell", "route-a")
                        .header(HttpHeaders.SET_COOKIE,
                                "route-cell=route-a; Path=/; Secure",
                                "sessionIdCookie=session-cookie; Path=/; Secure",
                                "sessionIdCookie.sig=session-signature; Path=/; Secure"));

        String expectedCookie = "route-cell=route-a; sessionIdCookie=session-cookie; "
                + "sessionIdCookie.sig=session-signature";
        fixture.server.expect(once(), requestTo(
                        "https://rnorder.kfc.com.cn/store-portal/api/v2/store/searchByLbs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-yumc-route-cell", "route-a"))
                .andExpect(header(HttpHeaders.COOKIE, expectedCookie))
                .andExpect(request -> {
                    assertThat(request.getHeaders()).doesNotContainKeys("kbck", "kbcts", "kbsv");
                    JsonNode body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    assertThat(body.get("sessionId").asText()).isEqualTo("session-1");
                    assertThat(body.get("mylat").asText()).isNotEqualTo("31.2304");
                    assertThat(body.get("encodeList").toString())
                            .isEqualTo("[\"mylat\",\"mylng\",\"mylatPhone\",\"mylngPhone\"]");
                })
                .andRespond(withSuccess("{\"code\":0,\"data\":{\"stores\":[]}}",
                        MediaType.APPLICATION_JSON)
                        .header("x-yumc-route-cell", "route-b")
                        .header(HttpHeaders.SET_COOKIE, "route-cell=route-b; Path=/; Secure"));

        var initialized = fixture.api.initializePreorder(fixture.context, fixture.location);
        assertThat(initialized.getSession().getSessionId()).isEqualTo("session-1");
        assertThat(initialized.getSession().cookieHeader()).isEqualTo(expectedCookie);
        JsonNode stores = fixture.api.searchStoresByLbs(fixture.context,
                initialized.getSession(), fixture.location, "");
        assertThat(stores.get("code").asInt()).isZero();
        assertThat(initialized.getSession().getRouteCell()).isEqualTo("route-b");
        assertThat(initialized.getSession().getCookies()).containsEntry("route-cell", "route-b");
        fixture.server.verify();
    }

    @Test
    void signsMenuWithShortPathAndPreservesValidatedStoreObject() throws IOException {
        Fixture fixture = fixture();
        var session = new com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession();
        session.setSessionId("session-1");
        session.setRouteCell("route-a");
        session.setCookies(new java.util.LinkedHashMap<>());
        session.getCookies().put("route-cell", "route-a");
        session.getCookies().put("sessionIdCookie", "session-cookie");
        session.getCookies().put("sessionIdCookie.sig", "session-signature");
        JsonNode store = mapper.readTree("{\"storecode\":\"TEST001\",\"storename\":\"Test\"}");

        fixture.server.expect(once(), requestTo(
                        "https://rnorder.kfc.com.cn/preorder-portal/api/v2/menu/list"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    JsonNode body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    assertThat(body.get("store")).isEqualTo(store);
                    assertThat(body.get("encodeList").isArray()).isTrue();
                    String expected = KfcRequestSignature.signPost("key", "secret", "123",
                            "/api/v2/menu/list", body.toString());
                    assertThat(request.getHeaders().getFirst("kbsv")).isEqualTo(expected);
                })
                .andRespond(withSuccess("{\"code\":0,\"data\":{\"menuData\":[]}}",
                        MediaType.APPLICATION_JSON));

        JsonNode response = fixture.api.menuList(fixture.context, session, store);
        assertThat(response.get("code").asInt()).isZero();
        fixture.server.verify();
    }

    @Test
    void searchesStoresByCityKeywordWithoutKbsHeaders() throws IOException {
        Fixture fixture = fixture();
        var session = new com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession();
        session.setSessionId("session-1");
        session.setRouteCell("route-a");
        session.setCookies(new java.util.LinkedHashMap<>());
        session.getCookies().put("route-cell", "route-a");
        session.getCookies().put("sessionIdCookie", "session-cookie");
        session.getCookies().put("sessionIdCookie.sig", "session-signature");

        fixture.server.expect(once(), requestTo(
                        "https://rnorder.kfc.com.cn/store-portal/api/v2/store/"
                                + "searchByCityCodeAndKeyword"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    assertThat(request.getHeaders()).doesNotContainKeys("kbck", "kbcts", "kbsv");
                    JsonNode body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    assertThat(body.get("keyword").asText()).isEqualTo("square");
                    assertThat(body.get("cityNameMy").asText()).isEqualTo("示例市");
                    assertThat(body.get("gbCityCode").asText()).isEqualTo("310000");
                    assertThat(body.get("mylat").asText()).isNotEqualTo("31.2304");
                    assertThat(body.get("encodeList").toString())
                            .isEqualTo("[\"mylat\",\"mylng\",\"mylatPhone\",\"mylngPhone\"]");
                })
                .andRespond(withSuccess("{\"code\":0,\"data\":{\"stores\":[]}}",
                        MediaType.APPLICATION_JSON));

        JsonNode response = fixture.api.searchStoresByCityCodeAndKeyword(fixture.context,
                session, fixture.location, "示例市", "square");

        assertThat(response.get("code").asInt()).isZero();
        fixture.server.verify();
    }

    private Fixture fixture() {
        AppLoginProperties credentials = new AppLoginProperties();
        credentials.setClientKey("key");
        credentials.setClientSecret("secret");
        credentials.setDesKey("12345678");
        credentials.setRequestSecretKey("request-secret");
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC);
        RnOrderAuthentication authentication = new RnOrderAuthentication(credentials, clock);
        UpstreamProperties upstream = new UpstreamProperties();
        upstream.setEnabled(true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        builder.requestInterceptor(new GzipResponseInterceptor());
        UpstreamGateway gateway = new UpstreamGateway(builder.build(), upstream, List.of(authentication));
        RnOrderApi api = new RnOrderApi(gateway, mapper, new AppLoginCrypto(credentials),
                credentials, new RnOrderProperties());
        RnOrderContext context = new RnOrderContext();
        context.setDeviceId("device-id");
        context.setUserCode("user-code");
        context.setTicket("upstream-ticket");
        context.setCityCode("310000");
        context.setUserAgent("test-agent");
        RnOrderLocation location = new RnOrderLocation();
        location.setLatitude("31.2304");
        location.setLongitude("121.4737");
        location.setGbCityCode("310000");
        return new Fixture(server, api, context, location);
    }

    /** 保存一次 Mock 上游测试所需的对象。 */
    private static final class Fixture {
        /** Mock HTTP 服务端。 */
        private final MockRestServiceServer server;
        /** 待测试的 RN 点餐客户端。 */
        private final RnOrderApi api;
        /** 已登录调用上下文。 */
        private final RnOrderContext context;
        /** 测试定位。 */
        private final RnOrderLocation location;

        private Fixture(MockRestServiceServer server, RnOrderApi api,
                        RnOrderContext context, RnOrderLocation location) {
            this.server = server;
            this.api = api;
            this.context = context;
            this.location = location;
        }
    }
}
