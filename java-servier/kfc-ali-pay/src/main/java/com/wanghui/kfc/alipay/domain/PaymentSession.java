package com.wanghui.kfc.alipay.domain;

import java.time.Instant;

/**
 * 一笔 KFC 支付宝支付的安全账本快照。
 *
 * @param bizId 调用方幂等业务号
 * @param orderId KFC 订单号
 * @param amountFen 支付金额，单位分
 * @param paymentContent KFC 收银台返回的支付宝 App SDK 订单串
 * @param paymentUrl 可选的支付宝 H5 临时链接
 * @param status 当前编排状态
 * @param clientOutcome 客户端最后一次上报结果
 * @param kfcOrderStatus KFC 服务端订单状态码
 * @param message 状态说明
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 */
public record PaymentSession(
        String bizId,
        String orderId,
        long amountFen,
        String paymentContent,
        String paymentUrl,
        PaymentStatus status,
        ClientPaymentOutcome clientOutcome,
        String kfcOrderStatus,
        String message,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * 创建新的幂等支付账本。
     *
     * @param bizId 调用方业务号
     * @param orderId KFC 订单号
     * @param amountFen 金额，单位分
     * @param paymentContent 支付宝 App SDK 订单串
     * @param paymentUrl 可选 H5 链接
     * @return 新账本
     */
    public static PaymentSession create(
            String bizId,
            String orderId,
            long amountFen,
            String paymentContent,
            String paymentUrl
    ) {
        Instant now = Instant.now();
        return new PaymentSession(
                bizId, orderId, amountFen, paymentContent, paymentUrl,
                PaymentStatus.CREATED, null, null, "created", now, now
        );
    }

    /**
     * 生成修改状态后的不可变快照。
     *
     * @param newStatus 新状态
     * @param outcome 客户端提示结果
     * @param upstreamStatus KFC 订单状态
     * @param newMessage 状态说明
     * @return 更新后的账本
     */
    public PaymentSession transition(
            PaymentStatus newStatus,
            ClientPaymentOutcome outcome,
            String upstreamStatus,
            String newMessage
    ) {
        return new PaymentSession(
                bizId, orderId, amountFen, paymentContent, paymentUrl,
                newStatus,
                outcome == null ? clientOutcome : outcome,
                upstreamStatus == null ? kfcOrderStatus : upstreamStatus,
                newMessage,
                createdAt,
                Instant.now()
        );
    }

    /**
     * 判断是否已经进入无需继续轮询的终态。
     *
     * @return 已支付、已取消或明确失败时返回 {@code true}
     */
    public boolean terminal() {
        return status == PaymentStatus.PAID
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.FAILED;
    }
}
