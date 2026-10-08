package com.wanghui.kfc.phhsapi.apploginphdappcn.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.basicapi.Brand;
import com.wanghui.kfc.basicapi.GzipResponseInterceptor;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import com.wanghui.kfc.basicapi.UpstreamProperties;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PhhsLoginProtocolTest {
    @Test
    void signsCapturedShortPathsAndTargetsOnlyPhhsDomain() throws Exception {
        PhhsLoginProperties properties = new PhhsLoginProperties();
        properties.setClientKey("key");
        properties.setClientSecret("secret");
        PhhsLoginAuthentication authentication = new PhhsLoginAuthentication(properties,
                Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC));
        UpstreamProperties upstreamProperties = new UpstreamProperties();
        upstreamProperties.getEnabled().put(Brand.PHHS, true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        builder.requestInterceptor(new GzipResponseInterceptor());
        UpstreamGateway gateway = new UpstreamGateway(
                builder.build(), upstreamProperties, authentication);
        ObjectMapper mapper = new ObjectMapper();

        String sendBody = "{\"phone\":\"cipher\",\"sendType\":3,\"mainBrand\":\"PHHS\"," 
                + "\"tdid\":\"test-tdid\",\"encodeList\":[\"phone\"],"
                + "\"isFromCustomerClient\":true,\"secretKey\":\"phhs\"}";
        server.expect(once(), requestTo("https://applogin.phdapp.cn/api/user/sendSmsCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(sendBody))
                .andExpect(header("Content-Type", "application/json; charset=utf-8"))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().getCharset())
                        .isEqualTo(StandardCharsets.UTF_8))
                .andExpect(header("Accept-Encoding", "gzip"))
                .andExpect(header("kbck", "key"))
                .andExpect(header("kbcts", "123"))
                .andExpect(header("kbsv", "bdeb4e35240306c9a0c3bdf0beb47077"))
                .andExpect(header("rcsav", "6.59.1"))
                .andRespond(withSuccess("{\"errCode\":0}", MediaType.APPLICATION_JSON));

        String loginBody = "{\"phone\":\"cipher-phone\",\"smsCode\":\"cipher-code\"," 
                + "\"deviceId\":\"test-device\",\"tdid\":\"test-tdid\","
                + "\"jPushRegId\":\"\",\"gbCityCode\":\"430800\","
                + "\"mainBrand\":\"PHHS\",\"encodeList\":[\"phone\",\"smsCode\"],"
                + "\"isFromCustomerClient\":true,\"secretKey\":\"phhs\"}";
        server.expect(once(), requestTo("https://applogin.phdapp.cn/api/user/loginBySmsCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(loginBody))
                .andExpect(header("kbsv", "48f7e5c4b6e721436cea71f1aa91075e"))
                .andRespond(withSuccess("{\"errCode\":0,\"data\":{}}", MediaType.APPLICATION_JSON));

        assertThat(gateway.post(PhhsUpstream.APP_LOGIN, "/api/user/sendSmsCode",
                mapper.readTree(sendBody), Map.of("rcsav", "6.59.1"))
                .path("errCode").asInt()).isZero();
        assertThat(gateway.post(PhhsUpstream.APP_LOGIN, "/api/user/loginBySmsCode",
                mapper.readTree(loginBody)).path("errCode").asInt()).isZero();
        server.verify();
    }
}
