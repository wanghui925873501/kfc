package com.wanghui.kfc.alipay.web.dto;

import com.wanghui.kfc.alipay.domain.PaymentSession;
import com.wanghui.kfc.alipay.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 返回给 UniApp 的支付宝交接信息。 */
@Data
@AllArgsConstructor
public class PaymentHandoffResponse {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public PaymentHandoffResponse() { }

    /** 业务号。 */
    private String bizId;
    /** KFC 订单号。 */
    private String orderId;
    /** 金额，单位分。 */
    private long amountFen;
    /** UniApp 支付 provider。 */
    private String provider;
    /** 支付宝 App SDK 订单串。 */
    @ToString.Exclude
    private String orderInfo;
    /** 当前编排状态。 */
    private PaymentStatus status;

    /**
     * 从内部支付账本生成客户端交接对象。
     * @param session 支付账本
     * @return 不包含管理令牌的客户端对象
     */
    public static PaymentHandoffResponse from(PaymentSession session) {
        return new PaymentHandoffResponse(session.bizId(), session.orderId(), session.amountFen(),
                "alipay", session.paymentContent(), session.status());
    }
}
