package com.wanghui.kfc.server.login;

import org.springframework.http.HttpStatus;

/** 表示可安全返回给前端、且不包含上游敏感报文的登录业务异常。 */
public class KfcLoginException extends RuntimeException {
    /** Java 序列化版本标识。 */
    private static final long serialVersionUID = 1L;
    /** 对外稳定的业务错误码。 */
    private final String code;
    /** 建议返回的 HTTP 状态。 */
    private final HttpStatus status;

    /**
     * 创建登录业务异常。
     *
     * @param code 对外稳定的业务错误码
     * @param message 不含手机号、验证码、token 或上游原始响应的提示
     * @param status 建议返回的 HTTP 状态
     */
    public KfcLoginException(String code, String message, HttpStatus status) {
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
