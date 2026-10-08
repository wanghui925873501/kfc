package com.wanghui.kfc.server.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在 MVC 拦截器运行前缓存 API 的 JSON 请求体，不记录其中的敏感字段。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class KfcRequestBodyCacheFilter extends OncePerRequestFilter {
    /** 供 Spring 创建 JSON 请求体缓存过滤器。 */
    public KfcRequestBodyCacheFilter() { }

    /**
     * 仅处理 API JSON 请求，查询参数形式的请求无需缓存 body。
     *
     * @param request 当前 HTTP 请求
     * @return 不需要缓存 JSON body 时为 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        return !request.getRequestURI().startsWith("/api/") || contentType == null
                || !contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 使用可重复读取的请求包装器继续过滤器链。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 下游 Servlet 处理失败时
     * @throws IOException 请求体读取或下游处理失败时
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }
}
