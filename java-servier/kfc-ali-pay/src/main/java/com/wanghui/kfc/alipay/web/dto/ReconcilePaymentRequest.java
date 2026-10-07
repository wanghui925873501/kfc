package com.wanghui.kfc.alipay.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 可信内部服务提交的 KFC 订单状态。 */
@Data
@AllArgsConstructor
public class ReconcilePaymentRequest {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public ReconcilePaymentRequest() { }

    /** KFC 订单号。 */
    @NotBlank
    private String orderId;
    /** KFC 状态码。 */
    @NotBlank
    private String status;
    /** 状态名称。 */
    @NotBlank
    private String statusName;
}
