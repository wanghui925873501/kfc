package com.wanghui.kfc.server.controller.login;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.server.login.KfcLoginResult;
import com.wanghui.kfc.server.login.KfcLoginService;
import com.wanghui.kfc.server.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 核验前端登录接口的路径、校验和脱敏响应。 */
class LoginControllerTest {
    @Test
    void returnsLocalSessionWhenSendEndpointFindsStoredToken() throws Exception {
        KfcLoginService service = mock(KfcLoginService.class);
        KfcLoginResult result = new KfcLoginResult();
        result.setLoggedIn(true);
        result.setReusedStoredToken(true);
        result.setSessionId("local-session");
        result.setInstallationId("installation-id");
        result.setExpiresInSeconds(28800L);
        when(service.sendSmsCode("13800000000")).thenReturn(result);
        MockMvc mvc = mvc(service);

        mvc.perform(post("/api/v1/login/sms-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedIn").value(true))
                .andExpect(jsonPath("$.data.smsSent").value(false))
                .andExpect(jsonPath("$.data.nextAction").value("USE_SESSION"))
                .andExpect(jsonPath("$.data.session.sessionId").value("local-session"))
                .andExpect(jsonPath("$.data.session.upstreamToken").doesNotExist());
    }

    @Test
    void rejectsInvalidLoginRequestWithoutEchoingSensitiveInput() throws Exception {
        KfcLoginService service = mock(KfcLoginService.class);
        MockMvc mvc = mvc(service);

        mvc.perform(post("/api/v1/login/sms-code/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"bad-phone\",\"smsCode\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("请求参数校验失败"));
    }

    private MockMvc mvc(KfcLoginService service) {
        return MockMvcBuilders.standaloneSetup(new LoginController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
}
