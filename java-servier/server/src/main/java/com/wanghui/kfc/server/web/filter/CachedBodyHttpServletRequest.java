package com.wanghui.kfc.server.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 缓存 JSON 请求体并允许拦截器与 Spring MVC 参数转换器分别读取。 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    /** 当前请求体的内存副本，仅存在于本次请求生命周期。 */
    private final byte[] cachedBody;

    /**
     * 读取并缓存原始请求体。
     *
     * @param request 原始 HTTP 请求
     * @throws IOException 请求体读取失败时
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * 返回请求体内存副本。
     *
     * @return 请求体副本，调用方修改不会影响后续读取
     */
    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    /**
     * 为每个读取方创建独立的请求体输入流。
     *
     * @return 可重复读取的 Servlet 输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            /** {@inheritDoc} */
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isReady() {
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public void setReadListener(ReadListener readListener) {
                // 内存流始终可以同步读取，不注册异步回调。
            }

            /** {@inheritDoc} */
            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 使用请求声明的字符集创建可重复读取的字符流。
     *
     * @return 请求体字符流
     */
    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
