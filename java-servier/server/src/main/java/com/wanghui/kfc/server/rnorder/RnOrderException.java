package com.wanghui.kfc.server.rnorder;

import org.springframework.http.HttpStatus;

/** 表示可安全返回给前端且不包含上游原始报文的 RN 点餐异常。 */
public class RnOrderException extends RuntimeException {
    /** Java 序列化版本标识。 */
    private static final long serialVersionUID = 1L;
    /** 对外稳定的业务错误码。 */
    private final String code;
    /** 建议返回的 HTTP 状态。 */
    private final HttpStatus status;

    /**
     * 创建 RN 点餐业务异常。
     *
     * @param code 对外稳定的错误码
     * @param message 不含 token、Cookie、设备标识或上游原文的提示
     * @param status 建议返回的 HTTP 状态
     */
    public RnOrderException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * 读取对外业务错误码。
     *
     * @return 业务错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 读取建议返回的 HTTP 状态。
     *
     * @return HTTP 状态
     */
    public HttpStatus getStatus() {
        return status;
    }
}
