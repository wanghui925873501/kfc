package com.wanghui.kfc.kfcapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** 将声明为 gzip 的上游响应解压后交给 JSON 转换器。 */
public final class GzipResponseInterceptor implements ClientHttpRequestInterceptor {
    /** 供 HTTP 客户端注册响应解压拦截器。 */
    public GzipResponseInterceptor() { }

    /**
     * 保留请求原样，按响应头决定是否解压响应正文。
     * @param request 即将发送的 HTTP 请求
     * @param body 已序列化的请求正文
     * @param execution 下一个拦截器或实际传输
     * @return 可由 JSON 转换器读取的响应
     * @throws IOException 传输或 gzip 解压失败时
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        String encoding = response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (encoding == null || !"gzip".equalsIgnoreCase(encoding.trim())) {
            return response;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(response.getBody())) {
            byte[] decoded = gzip.readAllBytes();
            return new DecodedResponse(response, decoded);
        } catch (IOException exception) {
            response.close();
            throw exception;
        }
    }

    /** 解压后的响应只改变正文及相应的编码、长度头。 */
    private static final class DecodedResponse implements ClientHttpResponse {
        /** 原始响应，用于读取状态和释放底层连接。 */
        private final ClientHttpResponse original;
        /** 已解压的正文，供转换器读取。 */
        private final byte[] body;
        /** 与已解压正文一致的响应头。 */
        private final HttpHeaders headers;

        private DecodedResponse(ClientHttpResponse original, byte[] body) {
            this.original = original;
            this.body = body;
            this.headers = new HttpHeaders();
            this.headers.addAll(original.getHeaders());
            this.headers.remove(HttpHeaders.CONTENT_ENCODING);
            this.headers.remove(HttpHeaders.CONTENT_LENGTH);
            this.headers.remove(HttpHeaders.TRANSFER_ENCODING);
            this.headers.setContentLength(body.length);
        }

        /**
         * 保留上游原始 HTTP 状态码。
         * @return 上游 HTTP 状态码
         * @throws IOException 底层响应读取失败时
         */
        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return original.getStatusCode();
        }

        /**
         * 保留上游原始状态描述。
         * @return 上游状态描述
         * @throws IOException 底层响应读取失败时
         */
        @Override
        public String getStatusText() throws IOException {
            return original.getStatusText();
        }

        /**
         * 返回与解压后正文一致的响应头。
         * @return 已移除 gzip 和旧传输长度的响应头
         */
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        /**
         * 返回可重复读取的解压后正文。
         * @return 解压后的字节流
         */
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        /** 释放原始响应的底层资源。 */
        @Override
        public void close() {
            original.close();
        }
    }
}
