package com.wanghui.kfc.phhsapi.apploginphdappcn.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsCaptchaProof;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginCrypto;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginProperties;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PhhsLoginApiTest {
    /** 构造和解析测试 JSON 的工具。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsCapturedSmsAndLoginBodiesInEvidenceOrder() throws Exception {
        PhhsLoginProperties properties = properties();
        PhhsLoginCrypto crypto = new PhhsLoginCrypto(properties);
        UpstreamGateway gateway = mock(UpstreamGateway.class);
        PhhsLoginApi api = new PhhsLoginApi(gateway, crypto, properties, mapper);
        PhhsLoginContext context = context();
        when(gateway.post(eq(PhhsUpstream.APP_LOGIN), any(), any(JsonNode.class), anyMap()))
                .thenReturn(mapper.readTree("{\"errCode\":0,\"data\":{\"mainBrandData\":"
                        + "{\"token\":\"test-token\",\"userCode\":\"test-user\"}}}"));

        api.sendSmsCode(new PhhsSendSmsCodeParam("13800000000", context));
        api.loginBySmsCode(new PhhsLoginBySmsCodeParam("13800000000", "123456", context));

        ArgumentCaptor<JsonNode> sendBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(PhhsUpstream.APP_LOGIN), eq("/api/user/sendSmsCode"),
                sendBody.capture(), eq(context.headers()));
        String encryptedPhone = crypto.encrypt("13800000000");
        assertThat(sendBody.getValue().toString()).isEqualTo("{\"phone\":\"" + encryptedPhone
                + "\",\"sendType\":3,\"mainBrand\":\"PHHS\",\"tdid\":\"test-tdid\"," 
                + "\"encodeList\":[\"phone\"],\"isFromCustomerClient\":true,"
                + "\"secretKey\":\"phhs\"}");

        ArgumentCaptor<JsonNode> loginBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(PhhsUpstream.APP_LOGIN), eq("/api/user/loginBySmsCode"),
                loginBody.capture(), eq(context.headers()));
        assertThat(loginBody.getValue().fieldNames()).toIterable().containsExactly(
                "phone", "smsCode", "deviceId", "tdid", "jPushRegId", "gbCityCode",
                "mainBrand", "encodeList", "isFromCustomerClient", "secretKey");
        assertThat(loginBody.getValue().path("phone").asText()).isEqualTo(encryptedPhone);
        assertThat(loginBody.getValue().path("smsCode").asText()).isNotEqualTo("123456");
        assertThat(loginBody.getValue().path("mainBrand").asText()).isEqualTo("PHHS");
        assertThat(loginBody.getValue().path("encodeList")).hasSize(2);
    }

    @Test
    void encryptsWithCapturedDesModeAndHidesSensitiveValues() throws Exception {
        PhhsLoginProperties properties = properties();
        PhhsLoginCrypto crypto = new PhhsLoginCrypto(properties);
        assertThat(crypto.encrypt("test-value")).isEqualTo("604xISIBN/T1giD2D4t9iQ==");

        PhhsLoginBySmsCodeParam param = new PhhsLoginBySmsCodeParam(
                "13800000000", "123456", context());
        assertThat(param.toString()).doesNotContain("13800000000", "123456", "test-device");

        PhhsLoginBySmsCodeVo response = mapper.readValue("{\"errCode\":0,\"data\":{"
                + "\"mainBrandData\":{\"token\":\"secret-token\","
                + "\"phone\":\"encrypted-phone\",\"userCode\":\"test-user\"}}}",
                PhhsLoginBySmsCodeVo.class);
        assertThat(response.getData().getMainBrandData().getToken()).isEqualTo("secret-token");
        assertThat(response.toString()).doesNotContain("secret-token", "encrypted-phone");
    }

    @Test
    void buildsApkDeclaredCaptchaRegistrationAndVerifiedBodies() throws Exception {
        PhhsLoginProperties properties = properties();
        PhhsLoginCrypto crypto = new PhhsLoginCrypto(properties);
        UpstreamGateway gateway = mock(UpstreamGateway.class);
        PhhsLoginApi api = new PhhsLoginApi(gateway, crypto, properties, mapper);
        PhhsLoginContext context = context();
        when(gateway.get(eq(PhhsUpstream.APP_LOGIN), any(), anyMap(), anyMap()))
                .thenReturn(mapper.readTree("{\"errCode\":0,\"data\":{}}"));
        when(gateway.post(eq(PhhsUpstream.APP_LOGIN), any(), any(JsonNode.class), anyMap()))
                .thenReturn(mapper.readTree("{\"errCode\":0}"));

        api.startCaptcha(1, context);
        verify(gateway).get(eq(PhhsUpstream.APP_LOGIN), eq("/api/svc/startCaptcha"),
                eq(java.util.Map.of("rt", "1", "type", "MOBILE", "ct", "native")),
                eq(context.headers()));

        PhhsCaptchaProof proof = proof();
        api.sendSmsCodeVerified(new PhhsSendSmsCodeParam("13800000000", context), proof);
        ArgumentCaptor<JsonNode> sendBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(PhhsUpstream.APP_LOGIN),
                eq("/api/svc/to/user/sendSmsCode"), sendBody.capture(), eq(context.headers()));
        assertThat(sendBody.getValue().fieldNames()).toIterable().containsExactly(
                "phone", "sendType", "mainBrand", "gtChallenge", "gtValidate", "gtSeccode",
                "userid", "gtServerStatus", "rt", "event_id", "tdid", "encodeList",
                "isFromCustomerClient", "secretKey");
        assertThat(sendBody.getValue().path("event_id").asText()).isEqualTo("test-event");

        api.loginBySmsCodeVerified(
                new PhhsLoginBySmsCodeParam("13800000000", "123456", context), proof);
        ArgumentCaptor<JsonNode> loginBody = ArgumentCaptor.forClass(JsonNode.class);
        verify(gateway).post(eq(PhhsUpstream.APP_LOGIN),
                eq("/api/svc/to/user/loginBySmsCode"), loginBody.capture(), eq(context.headers()));
        assertThat(loginBody.getValue().fieldNames()).toIterable().containsExactly(
                "phone", "smsCode", "deviceId", "tdid", "jPushRegId", "gbCityCode",
                "mainBrand", "subBrands", "gtChallenge", "gtValidate", "gtSeccode", "userid",
                "gtServerStatus", "rt", "event_id", "encodeList", "isFromCustomerClient",
                "secretKey");
        assertThat(loginBody.getValue().path("subBrands").isNull()).isTrue();
    }

    private static PhhsLoginProperties properties() {
        PhhsLoginProperties properties = new PhhsLoginProperties();
        properties.setDesKey("12345678");
        properties.setRequestSecretKey("phhs");
        return properties;
    }

    private static PhhsLoginContext context() {
        return new PhhsLoginContext("test-tdid", "test-device", "", "430800", "app", "",
                "test-rcsdcid", "6.59.1", "test-agent");
    }

    private static PhhsCaptchaProof proof() {
        PhhsCaptchaProof proof = new PhhsCaptchaProof();
        proof.setRt(1);
        proof.setEventId("test-event");
        proof.setUserid("test-user");
        proof.setGtServerStatus("1");
        proof.setGtChallenge("test-challenge");
        proof.setGtValidate("test-validate");
        proof.setGtSeccode("test-seccode");
        return proof;
    }
}
