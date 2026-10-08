package com.wanghui.kfc.server.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import com.wanghui.kfc.server.context.KfcRequestContextService;
import com.wanghui.kfc.server.login.KfcLoginException;
import com.wanghui.kfc.server.web.annotation.KfcLoginOptional;
import com.wanghui.kfc.server.web.filter.CachedBodyHttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

/** 核验手机号拦截器的请求体读取、登录放行和普通接口拒绝分支。 */
class KfcUserContextInterceptorTest {
    @Test
    void loadsContextFromJsonWithoutConsumingControllerBody() throws Exception {
        KfcRequestContextService service = mock(KfcRequestContextService.class);
        KfcRequestContext context = new KfcRequestContext();
        context.setPhone("13800000000");
        when(service.load("13800000000")).thenReturn(Optional.of(context));
        KfcRequestContextHolder holder = new KfcRequestContextHolder();
        KfcUserContextInterceptor interceptor = new KfcUserContextInterceptor(service, holder);
        CachedBodyHttpServletRequest request = request("{\"phone\":\"13800000000\",\"value\":1}");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler("required")))
                .isTrue();
        assertThat(holder.get()).isSameAs(context);
        assertThat(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .contains("13800000000", "\"value\":1");
        interceptor.afterCompletion(request, new MockHttpServletResponse(), handler("required"), null);
        assertThat(holder.get()).isNull();
    }

    @Test
    void allowsUnloggedPhoneOnlyForLoginOptionalHandler() throws Exception {
        KfcRequestContextService service = mock(KfcRequestContextService.class);
        when(service.load("13800000000")).thenReturn(Optional.empty());
        KfcUserContextInterceptor interceptor = new KfcUserContextInterceptor(
                service, new KfcRequestContextHolder());
        CachedBodyHttpServletRequest request = request("{\"phone\":\"13800000000\"}");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler("optional")))
                .isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(),
                handler("required")))
                .isInstanceOf(KfcLoginException.class)
                .extracting("code").isEqualTo("LOGIN_REQUIRED");
    }

    @Test
    void rejectsMissingPhoneBeforeControllerInvocation() throws Exception {
        KfcUserContextInterceptor interceptor = new KfcUserContextInterceptor(
                mock(KfcRequestContextService.class), new KfcRequestContextHolder());

        assertThatThrownBy(() -> interceptor.preHandle(request("{\"value\":1}"),
                new MockHttpServletResponse(), handler("optional")))
                .isInstanceOf(KfcLoginException.class)
                .extracting("code").isEqualTo("PHONE_REQUIRED");
    }

    private CachedBodyHttpServletRequest request(String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new CachedBodyHttpServletRequest(request);
    }

    private HandlerMethod handler(String name) throws Exception {
        Method method = TestHandler.class.getDeclaredMethod(name);
        return new HandlerMethod(new TestHandler(), method);
    }

    private static class TestHandler {
        @KfcLoginOptional
        void optional() { }

        void required() { }
    }
}
