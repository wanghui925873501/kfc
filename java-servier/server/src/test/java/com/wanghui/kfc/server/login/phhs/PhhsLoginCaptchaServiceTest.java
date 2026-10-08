package com.wanghui.kfc.server.login.phhs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Client;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Result;
import com.wanghui.kfc.phhsapi.apploginphdappcn.api.PhhsLoginApi;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsCaptchaProof;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsSendSmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsStartCaptchaVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 核验 PHHS 单次挑战编排边界，不请求品牌上游或本机识别服务。 */
class PhhsLoginCaptchaServiceTest {
    /** 仅创建合成上游响应。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void retriesSmsExactlyOnceThroughXiaohuiService() throws Exception {
        PhhsLoginApi api = mock(PhhsLoginApi.class);
        Captcha3Client client = mock(Captcha3Client.class);
        PhhsSendSmsCodeParam request = new PhhsSendSmsCodeParam(
                "test-phone", new PhhsLoginContext());
        PhhsSendSmsCodeVo risk = new PhhsSendSmsCodeVo();
        risk.setErrCode(5910060);
        risk.setErrData(mapper.readTree("{\"data\":\"test-event\"}"));
        when(api.sendSmsCode(request)).thenReturn(risk);
        when(api.startCaptcha(1, request.getContext())).thenReturn(registration());
        when(client.solve("test-gt", "test-challenge")).thenReturn(solved());
        PhhsSendSmsCodeVo success = new PhhsSendSmsCodeVo();
        success.setErrCode(0);
        when(api.sendSmsCodeVerified(any(), any())).thenReturn(success);

        assertThat(new PhhsLoginCaptchaService(api, client).sendSmsCode(request))
                .isSameAs(success);
        ArgumentCaptor<PhhsCaptchaProof> proof =
                ArgumentCaptor.forClass(PhhsCaptchaProof.class);
        verify(api).sendSmsCodeVerified(org.mockito.ArgumentMatchers.eq(request), proof.capture());
        assertThat(proof.getValue().getEventId()).isEqualTo("test-event");
        assertThat(proof.getValue().getRt()).isEqualTo(1);
        assertThat(proof.getValue().getGtValidate()).isEqualTo("test-validate");
    }

    @Test
    void retriesLoginExactlyOnceForSecondRiskType() throws Exception {
        PhhsLoginApi api = mock(PhhsLoginApi.class);
        Captcha3Client client = mock(Captcha3Client.class);
        PhhsLoginBySmsCodeParam request = new PhhsLoginBySmsCodeParam(
                "test-phone", "test-code", new PhhsLoginContext());
        PhhsLoginBySmsCodeVo risk = new PhhsLoginBySmsCodeVo();
        risk.setErrCode(5910061);
        risk.setErrData(mapper.readTree("{\"data\":\"test-event\"}"));
        when(api.loginBySmsCode(request)).thenReturn(risk);
        when(api.startCaptcha(2, request.getContext())).thenReturn(registration());
        when(client.solve("test-gt", "test-challenge")).thenReturn(solved());
        PhhsLoginBySmsCodeVo success = new PhhsLoginBySmsCodeVo();
        success.setErrCode(0);
        when(api.loginBySmsCodeVerified(any(), any())).thenReturn(success);

        assertThat(new PhhsLoginCaptchaService(api, client).loginBySmsCode(request))
                .isSameAs(success);
        ArgumentCaptor<PhhsCaptchaProof> proof =
                ArgumentCaptor.forClass(PhhsCaptchaProof.class);
        verify(api).loginBySmsCodeVerified(org.mockito.ArgumentMatchers.eq(request), proof.capture());
        assertThat(proof.getValue().getRt()).isEqualTo(2);
    }

    @Test
    void stopsWithoutRetryWhenRegistrationFails() throws Exception {
        PhhsLoginApi api = mock(PhhsLoginApi.class);
        Captcha3Client client = mock(Captcha3Client.class);
        PhhsSendSmsCodeParam request = new PhhsSendSmsCodeParam(
                "test-phone", new PhhsLoginContext());
        PhhsSendSmsCodeVo risk = new PhhsSendSmsCodeVo();
        risk.setErrCode(5910060);
        risk.setErrData(mapper.readTree("{\"data\":\"test-event\"}"));
        when(api.sendSmsCode(request)).thenReturn(risk);
        when(api.startCaptcha(1, request.getContext())).thenReturn(new PhhsStartCaptchaVo());

        assertThatThrownBy(() -> new PhhsLoginCaptchaService(api, client).sendSmsCode(request))
                .hasMessageContaining("注册数据不完整");
        verify(client, never()).solve(any(), any());
        verify(api, never()).sendSmsCodeVerified(any(), any());
    }

    private PhhsStartCaptchaVo registration() throws Exception {
        PhhsStartCaptchaVo registration = new PhhsStartCaptchaVo();
        registration.setErrCode(0);
        registration.setData(mapper.readTree("{\"gt\":\"test-gt\"," 
                + "\"challenge\":\"test-challenge\",\"userid\":\"test-user\"," 
                + "\"gtServerStatus\":1}"));
        return registration;
    }

    private static Captcha3Result solved() {
        Captcha3Result solved = new Captcha3Result();
        solved.setChallenge("new-challenge");
        solved.setValidate("test-validate");
        solved.setSeccode("test-validate|jordan");
        return solved;
    }
}
