package com.wanghui.kfc.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一的 HTTP 响应数据结构。
 * @param <T> 业务数据类型
 */
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public ApiResponse() { }

    /** 业务状态码。 */
    private String code;
    /** 面向调用方的简短说明。 */
    private String message;
    /** 业务数据；失败时通常为空。 */
    private T data;

    /**
     * 创建成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    /**
     * 创建不包含业务数据的失败响应。
     *
     * @param code 业务错误码
     * @param message 错误说明
     * @return 失败响应
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
