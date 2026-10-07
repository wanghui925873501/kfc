package com.wanghui.kfc.kfcapi.apploginkfcappcn.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.kfcapi.Upstream;
import com.wanghui.kfc.kfcapi.UpstreamGateway;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.LoginBySmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AppLoginApiTest {
    /** 构造和解析测试 JSON 的工具。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsObservedSmsRequestsWithoutPlaintextCredentials() throws Exception {
        AppLoginProperties properties = new AppLoginProperties();
        properties.setDesKey("12345678");
        properties.setRequestSecretKey("test-body-value");
        UpstreamGateway gateway = mock(UpstreamGateway.class);
        AppLoginApi api = new AppLoginApi(gateway, new AppLoginCrypto(properties), properties, mapper);
        AppLoginContext context = new AppLoginContext("test-tdid", "test-device", "", "test-city",
                "test-channel", "", "test-rcsdcid", "test-rcsav", "test-agent");
        when(gateway.post(eq(Upstream.APP_LOGIN), any(), any(JsonNode.class), anyMap()))
                .thenReturn(mapper.readTree("{\"errCode\":0,\"data\":{\"mainBrandData\":{\"token\":\"test-token\"}}}"));

        api.sendSmsCode(new SendSmsCodeParam("test-value", context));
        api.loginBySmsCode(new LoginBySmsCodeParam("test-value", "test-code", context));

        ArgumentCaptor<JsonNode> sendBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(Upstream.APP_LOGIN), eq("/api/user/sendSmsCode"), sendBody.capture(),
                eq(context.headers()));
        assertThat(sendBody.getValue().path("phone").asText()).isEqualTo("604xISIBN/T1giD2D4t9iQ==");
        assertThat(sendBody.getValue().path("sendType").asInt()).isEqualTo(3);
        assertThat(sendBody.getValue().path("encodeList").get(0).asText()).isEqualTo("phone");
        assertThat(sendBody.getValue().toString()).isEqualTo("{\"phone\":\"604xISIBN/T1giD2D4t9iQ==\","
                + "\"sendType\":3,\"mainBrand\":\"KFC\",\"tdid\":\"test-tdid\","
                + "\"encodeList\":[\"phone\"],\"isFromCustomerClient\":true,"
                + "\"secretKey\":\"test-body-value\"}");

        ArgumentCaptor<JsonNode> loginBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(Upstream.APP_LOGIN), eq("/api/user/loginBySmsCode"), loginBody.capture(),
                eq(context.headers()));
        assertThat(loginBody.getValue().path("phone").asText()).isNotEqualTo("test-value");
        assertThat(loginBody.getValue().path("smsCode").asText()).isNotEqualTo("test-code");
        assertThat(loginBody.getValue().path("deviceId").asText()).isEqualTo("test-device");
        assertThat(loginBody.getValue().path("encodeList").size()).isEqualTo(2);
    }

    @Test
    void preservesRiskChallengeWithoutTreatingItAsSmsSuccess() throws Exception {
        SendSmsCodeVo response = mapper.readValue("{\"errCode\":\"5910060\","
                + "\"errData\":{\"data\":\"synthetic-event\"},\"errMsg\":\"RCS Failed\"}",
                SendSmsCodeVo.class);

        assertThat(response.requiresHumanVerification()).isTrue();
        assertThat(response.getErrData().path("data").asText()).isEqualTo("synthetic-event");
        assertThat(response.toString()).doesNotContain("synthetic-event");
        assertThat(mapper.readValue("{\"errCode\":0}", SendSmsCodeVo.class)
                .requiresHumanVerification()).isFalse();
    }
}
