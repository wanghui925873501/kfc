package com.wanghui.kfc.server.web.filter;

import com.wanghui.kfc.server.web.logging.ApiHttpLogProperties;
import com.wanghui.kfc.server.web.logging.ApiLogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** 统一记录所有 API Controller 的脱敏请求参数、返回参数、状态码和处理耗时。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRequestResponseLoggingFilter extends OncePerRequestFilter {
    /** API 请求响应日志输出器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestResponseLoggingFilter.class);
    /** 写入 MDC 和响应头的请求 ID 名称。 */
    private static final String REQUEST_ID = "requestId";
    /** 返回给前端用于关联服务端日志的请求头。 */
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** HTTP 日志配置。 */
    private final ApiHttpLogProperties properties;
    /** JSON 与查询参数脱敏器。 */
    private final ApiLogSanitizer sanitizer;

    /**
     * 创建统一 API 请求响应日志过滤器。
     *
     * @param properties HTTP 日志配置
     * @param sanitizer JSON 与查询参数脱敏器
     */
    public ApiRequestResponseLoggingFilter(ApiHttpLogProperties properties, ApiLogSanitizer sanitizer) {
        this.properties = properties;
        this.sanitizer = sanitizer;
    }

    /**
     * 仅记录 `/api/**` 请求，并允许通过配置整体关闭。
     *
     * @param request 当前 HTTP 请求
     * @return 无需记录日志时为 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || !request.getRequestURI().startsWith("/api/");
    }

    /**
     * 在 Controller 调用前后分别记录脱敏请求与响应，并把缓存响应原样写回客户端。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续过滤器与 Servlet 链
     * @throws ServletException 下游 Servlet 处理失败时
     * @throws IOException 请求或响应读写失败时
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        MDC.put(REQUEST_ID, requestId);
        responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);
        logRequest(request, requestId);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            try {
                logResponse(responseWrapper, requestId, startedAt);
            } finally {
                try {
                    responseWrapper.copyBodyToResponse();
                } finally {
                    MDC.remove(REQUEST_ID);
                }
            }
        }
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        String query = sanitizer.sanitizeParameters(request.getParameterMap(), properties.getMaxBodyLength());
        String body = requestBody(request);
        LOGGER.info("[API_REQUEST] requestId={} method={} uri={} query={} body={}",
                requestId, request.getMethod(), request.getRequestURI(), query, body);
    }

    private void logResponse(ContentCachingResponseWrapper response, String requestId, long startedAt) {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        String body = responseBody(response);
        LOGGER.info("[API_RESPONSE] requestId={} status={} durationMs={} body={}",
                requestId, response.getStatus(), durationMillis, body);
    }

    private String requestBody(HttpServletRequest request) {
        if (!(request instanceof CachedBodyHttpServletRequest cached)) return "<not-cached>";
        if (!isJson(request.getContentType())) return "<non-json>";
        return sanitizer.sanitizeJson(decode(cached.getCachedBody(), request.getCharacterEncoding()),
                properties.getMaxBodyLength());
    }

    private String responseBody(ContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) return "<empty>";
        if (!isJson(response.getContentType())) return "<non-json>";
        return sanitizer.sanitizeJson(decode(body, response.getCharacterEncoding()),
                properties.getMaxBodyLength());
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private String decode(byte[] body, String encoding) {
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new String(body, charset);
    }
}
