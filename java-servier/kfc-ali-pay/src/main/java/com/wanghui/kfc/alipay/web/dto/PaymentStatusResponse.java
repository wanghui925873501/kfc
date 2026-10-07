package com.wanghui.kfc.alipay.web.dto;

import com.wanghui.kfc.alipay.domain.ClientPaymentOutcome;
import com.wanghui.kfc.alipay.domain.PaymentSession;
import com.wanghui.kfc.alipay.domain.PaymentStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 不暴露支付订单串和临时 H5 链接的支付状态响应。 */
@Data
@AllArgsConstructor
public class PaymentStatusResponse {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public PaymentStatusResponse() { }

    /** 业务号。 */
    private String bizId;
    /** KFC 订单号。 */
    private String orderId;
    /** 金额，单位分。 */
    private long amountFen;
    /** 编排状态。 */
    private PaymentStatus status;
    /** 客户端提示结果。 */
    private ClientPaymentOutcome clientOutcome;
    /** KFC 服务端状态。 */
    private String kfcOrderStatus;
    /** 状态说明。 */
    private String message;
    /** 最后更新时间。 */
    private Instant updatedAt;

    /**
     * 从内部账本创建脱敏状态对象。
     * @param session 内部支付账本
     * @return 脱敏响应
     */
    public static PaymentStatusResponse from(PaymentSession session) {
        return new PaymentStatusResponse(session.bizId(), session.orderId(), session.amountFen(),
                session.status(), session.clientOutcome(), session.kfcOrderStatus(),
                session.message(), session.updatedAt());
    }
}
