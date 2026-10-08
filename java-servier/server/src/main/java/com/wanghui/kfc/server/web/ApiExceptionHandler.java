package com.wanghui.kfc.server.web;

import com.wanghui.kfc.common.ApiResponse;
import com.wanghui.kfc.kfcapi.UpstreamException;
import com.wanghui.kfc.server.login.KfcLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** 将可预期异常转换为统一、无凭据泄露的 HTTP 响应。 */
@RestControllerAdvice
public class ApiExceptionHandler {
    /** 供 Spring 创建全局异常处理器。 */
    public ApiExceptionHandler() { }

    /**
     * 处理本地请求参数错误。
     *
     * @param e 参数错误
     * @return HTTP 400 响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
    }

    /**
     * 处理 Bean Validation 拒绝的前端请求，不回显手机号或验证码。
     *
     * @param e 参数校验异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "请求参数校验失败"));
    }

    /**
     * 处理已经脱敏并带稳定业务码的登录异常。
     *
     * @param e 登录业务异常
     * @return 异常指定的安全 HTTP 响应
     */
    @ExceptionHandler(KfcLoginException.class)
    public ResponseEntity<ApiResponse<Void>> login(KfcLoginException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理上游未启用、未配置或鉴权未就绪的情况。
     *
     * @param e 不可用原因
     * @return HTTP 503 响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("UPSTREAM_DISABLED", e.getMessage()));
    }

    /**
     * 处理上游错误或连接失败，不回传原始敏感响应。
     *
     * @param e 上游调用异常
     * @return HTTP 502 响应
     */
    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ApiResponse<Void>> upstream(UpstreamException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("UPSTREAM_ERROR", e.getMessage()));
    }
}
