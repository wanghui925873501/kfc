package com.wanghui.kfc.alipay.domain;

/**
 * 支付编排状态；只有 KFC 服务端订单查询可以把会话确认成已支付。
 */
public enum PaymentStatus {
    /** 已创建，尚未交给客户端。 */
    CREATED,
    /** 已生成 UniApp 或浏览器交接数据。 */
    HANDOFF_READY,
    /** 客户端已唤起支付宝，结果仍不可信。 */
    PAYMENT_LAUNCHED,
    /** 已收到客户端结果，等待 KFC 服务端确认。 */
    CONFIRMING,
    /** KFC 服务端确认已支付。 */
    PAID,
    /** KFC 服务端确认订单已取消。 */
    CANCELLED,
    /** 支付明确失败或订单过期。 */
    FAILED,
    /** 结果不明确，需要继续查询或人工核对。 */
    PENDING_CONFIRMATION
}
