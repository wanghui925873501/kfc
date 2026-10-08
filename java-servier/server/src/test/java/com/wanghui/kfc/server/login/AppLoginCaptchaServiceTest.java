package com.wanghui.kfc.server.login;

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
import com.wanghui.kfc.kfcapi.apploginkfcappcn.api.AppLoginApi;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.CaptchaProof;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.StartCaptchaVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 核验单次挑战的流程边界，不请求 KFC 或在线识别服务。 */
class AppLoginCaptchaServiceTest {
    /** 仅创建合成上游响应。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void retriesSmsExactlyOnceWithSameEvent() throws Exception {
        AppLoginApi api = mock(AppLoginApi.class);
        Captcha3Client client = mock(Captcha3Client.class);
        SendSmsCodeParam request = new SendSmsCodeParam("test-phone", new AppLoginContext());
        SendSmsCodeVo risk = new SendSmsCodeVo();
        risk.setErrCode(5910060);
        risk.setErrData(mapper.readTree("{\"data\":\"test-event\"}"));
        StartCaptchaVo registration = new StartCaptchaVo();
        registration.setErrCode(0);
        registration.setData(mapper.readTree("{\"gt\":\"test-gt\","
                + "\"challenge\":\"test-challenge\",\"userid\":\"test-user\","
                + "\"gtServerStatus\":1}"));
        Captcha3Result solved = new Captcha3Result();
        solved.setChallenge("new-challenge");
        solved.setValidate("test-validate");
        solved.setSeccode("test-validate|jordan");
        SendSmsCodeVo success = new SendSmsCodeVo();
        success.setErrCode(0);
        when(api.sendSmsCode(request)).thenReturn(risk);
        when(api.startCaptcha(1, request.getContext())).thenReturn(registration);
        when(client.solve("test-gt", "test-challenge")).thenReturn(solved);
        when(api.sendSmsCodeVerified(any(), any())).thenReturn(success);

        assertThat(new AppLoginCaptchaService(api, client).sendSmsCode(request)).isSameAs(success);
        ArgumentCaptor<CaptchaProof> proof = ArgumentCaptor.forClass(CaptchaProof.class);
        verify(api).sendSmsCodeVerified(org.mockito.ArgumentMatchers.eq(request), proof.capture());
        assertThat(proof.getValue().getEventId()).isEqualTo("test-event");
        assertThat(proof.getValue().getGtChallenge()).isEqualTo("new-challenge");
        assertThat(proof.getValue().getRt()).isEqualTo(1);
    }

    @Test
    void stopsWithoutRetryWhenRegistrationFails() throws Exception {
        AppLoginApi api = mock(AppLoginApi.class);
        Captcha3Client client = mock(Captcha3Client.class);
        SendSmsCodeParam request = new SendSmsCodeParam("test-phone", new AppLoginContext());
        SendSmsCodeVo risk = new SendSmsCodeVo();
        risk.setErrCode(5910061);
        risk.setErrData(mapper.readTree("{\"data\":\"test-event\"}"));
        when(api.sendSmsCode(request)).thenReturn(risk);
        when(api.startCaptcha(2, request.getContext())).thenReturn(new StartCaptchaVo());
        assertThatThrownBy(() -> new AppLoginCaptchaService(api, client).sendSmsCode(request))
                .hasMessageContaining("注册数据不完整");
        verify(client, never()).solve(any(), any());
        verify(api, never()).sendSmsCodeVerified(any(), any());
    }
}
