package com.wanghui.kfc.alipay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 创建支付会话的请求。 */
@Data
@AllArgsConstructor
public class BeginPaymentRequest {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public BeginPaymentRequest() { }

    /** 调用方幂等业务号。 */
    @NotBlank
    private String bizId;
    /** KFC 订单号。 */
    @NotBlank
    private String orderId;
    /** 金额，单位分。 */
    @Positive
    private long amountFen;
    /** KFC {@code unifiedOrderPay} 返回的支付宝订单串。 */
    @NotBlank
    @ToString.Exclude
    private String paymentContent;
    /** 可选 H5 临时链接，仅用于浏览器人工交接。 */
    @ToString.Exclude
    private String paymentUrl;
}
