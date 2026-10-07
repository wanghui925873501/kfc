package com.wanghui.kfc.alipay.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将支付会话参数和状态错误转换为稳定的 HTTP Problem 响应。
 */
@RestControllerAdvice
public class PaymentExceptionHandler {

    /**
     * 创建支付异常响应处理器。
     */
    public PaymentExceptionHandler() {
        // 无状态处理器无需额外初始化。
    }

    /**
     * 处理无效业务参数。
     *
     * @param exception 参数或状态异常
     * @return HTTP 400 问题详情
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * 处理未启用的可选能力。
     *
     * @param exception 配置状态异常
     * @return HTTP 409 问题详情
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
