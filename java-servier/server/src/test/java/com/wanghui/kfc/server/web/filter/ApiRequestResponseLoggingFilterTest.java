package com.wanghui.kfc.server.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.wanghui.kfc.server.web.logging.ApiHttpLogProperties;
import com.wanghui.kfc.server.web.logging.ApiLogSanitizer;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 验证统一 API 日志不会消费 Controller 请求体或改变返回内容。 */
@ExtendWith(OutputCaptureExtension.class)
class ApiRequestResponseLoggingFilterTest {
    /** HTTP 日志开关与长度配置。 */
    private final ApiHttpLogProperties properties = new ApiHttpLogProperties();
    /** 被测试的统一 API 请求响应日志过滤器。 */
    private final ApiRequestResponseLoggingFilter filter = new ApiRequestResponseLoggingFilter(
            properties, new ApiLogSanitizer());

    /**
     * 请求响应均被记录并脱敏，同时原始报文仍完整交给 Controller 和客户端。
     *
     * @param output 测试期间捕获的应用日志
     * @throws Exception Servlet 模拟链执行失败时
     */
    @Test
    void logsSanitizedRequestAndResponseWithoutChangingPayload(CapturedOutput output) throws Exception {
        MockHttpServletRequest original = new MockHttpServletRequest("POST", "/api/v1/login/sms-code/send");
        original.setContentType(MediaType.APPLICATION_JSON_VALUE);
        original.setCharacterEncoding(StandardCharsets.UTF_8.name());
        original.setContent("{\"phone\":\"13800000000\",\"smsCode\":\"012345\"}"
                .getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(original);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(servletRequest.getReader().readLine())
                    .isEqualTo("{\"phone\":\"13800000000\",\"smsCode\":\"012345\"}");
            servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            servletResponse.getWriter().write(
                    "{\"code\":\"OK\",\"data\":{\"token\":\"token-value\"}}");
        });

        assertThat(response.getContentAsString())
                .isEqualTo("{\"code\":\"OK\",\"data\":{\"token\":\"token-value\"}}");
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(output).contains("[API_REQUEST]", "[API_RESPONSE]", "138****0000");
        assertThat(output).doesNotContain("13800000000", "012345", "token-value");
    }

    /** 配置关闭时过滤器不增加请求 ID，也不输出 API 日志。 */
    @Test
    void skipsLoggingWhenDisabled(CapturedOutput output) throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletResponse.getWriter().write("ok"));

        assertThat(response.getContentAsString()).isEqualTo("ok");
        assertThat(response.getHeader("X-Request-Id")).isNull();
        assertThat(output).doesNotContain("[API_REQUEST]", "[API_RESPONSE]");
    }

    /**
     * Controller 返回错误状态和统一错误体时仍记录状态、业务码和请求 ID。
     *
     * @param output 测试期间捕获的应用日志
     * @throws Exception Servlet 模拟链执行失败时
     */
    @Test
    void logsControllerErrorResponse(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/example");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"phone\":\"13800000000\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new CachedBodyHttpServletRequest(request), response,
                (servletRequest, servletResponse) -> {
                    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                    httpResponse.setStatus(503);
                    httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    httpResponse.getWriter().write(
                            "{\"code\":\"UPSTREAM_DISABLED\",\"message\":\"disabled\",\"data\":null}");
                });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("UPSTREAM_DISABLED");
        assertThat(output).contains("[API_RESPONSE]", "status=503", "UPSTREAM_DISABLED");
        assertThat(output).doesNotContain("13800000000");
    }
}
