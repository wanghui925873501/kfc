package com.wanghui.kfc.alipay.application;

import com.wanghui.kfc.alipay.domain.ClientPaymentOutcome;
import com.wanghui.kfc.alipay.domain.PaymentSession;
import com.wanghui.kfc.alipay.domain.PaymentStatus;
import com.wanghui.kfc.alipay.repository.PaymentSessionRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 编排支付宝交接、客户端提示和 KFC 服务端最终确认。
 */
@Service
public class PaymentOrchestrationService {

    /** 支付账本。 */
    private final PaymentSessionRepository repository;

    /** KFC 服务端订单查询客户端。 */
    private final KfcOrderStatusClient orderStatusClient;

    /**
     * 创建支付编排服务。
     *
     * @param repository 支付账本
     * @param orderStatusClient KFC 订单查询客户端
     */
    public PaymentOrchestrationService(
            PaymentSessionRepository repository,
            KfcOrderStatusClient orderStatusClient
    ) {
        this.repository = repository;
        this.orderStatusClient = orderStatusClient;
    }

    /**
     * 幂等创建支付会话。
     *
     * @param bizId 调用方业务号
     * @param orderId KFC 订单号
     * @param amountFen 金额，单位分
     * @param paymentContent 支付宝 App SDK 订单串
     * @param paymentUrl 可选 H5 链接
     * @return 已存在或新建的会话
     */
    public PaymentSession create(
            String bizId,
            String orderId,
            long amountFen,
            String paymentContent,
            String paymentUrl
    ) {
        PaymentSession existing = repository.findByBizId(bizId).orElse(null);
        if (existing != null) {
            if (!existing.orderId().equals(orderId) || existing.amountFen() != amountFen) {
                throw new IllegalArgumentException("bizId already belongs to another order or amount");
            }
            return existing;
        }
        return repository.save(
                PaymentSession.create(bizId, orderId, amountFen, paymentContent, paymentUrl)
                        .transition(PaymentStatus.HANDOFF_READY, null, null, "ready for UniApp handoff")
        );
    }

    /**
     * 标记客户端已经调用支付宝能力。
     *
     * @param bizId 业务号
     * @return 更新后的会话
     */
    public PaymentSession markLaunched(String bizId) {
        PaymentSession current = require(bizId);
        if (current.terminal()) {
            return current;
        }
        return repository.save(current.transition(
                PaymentStatus.PAYMENT_LAUNCHED, null, null, "client launched Alipay"
        ));
    }

    /**
     * 接收客户端提示，但不直接认定付款成功。
     *
     * @param bizId 业务号
     * @param outcome 客户端提示结果
     * @param message 非敏感说明
     * @return 等待服务端确认的会话
     */
    public PaymentSession acceptClientOutcome(
            String bizId,
            ClientPaymentOutcome outcome,
            String message
    ) {
        PaymentSession current = require(bizId);
        if (current.terminal()) {
            return current;
        }
        return repository.save(current.transition(
                PaymentStatus.CONFIRMING,
                outcome,
                null,
                message == null || message.isBlank() ? "waiting for KFC confirmation" : message
        ));
    }

    /**
     * 使用可信 KFC 订单状态更新支付账本。
     *
     * @param bizId 业务号
     * @param snapshot KFC 订单状态
     * @return 更新后的会话
     */
    public PaymentSession reconcile(String bizId, KfcOrderSnapshot snapshot) {
        PaymentSession current = require(bizId);
        if (!current.orderId().equals(snapshot.getOrderId())) {
            throw new IllegalArgumentException("KFC orderId does not match payment session");
        }
        PaymentStatus mapped = switch (snapshot.getStatus()) {
            case "102" -> PaymentStatus.PAID;
            case "504" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.CONFIRMING;
        };
        return repository.save(current.transition(
                mapped,
                null,
                snapshot.getStatus(),
                snapshot.getStatusName()
        ));
    }

    /**
     * 主动查询一次 KFC 订单状态。
     *
     * @param bizId 业务号
     * @return 查询后或原有会话
     */
    public PaymentSession reconcileNow(String bizId) {
        PaymentSession current = require(bizId);
        return orderStatusClient.query(current.orderId())
                .map(snapshot -> reconcile(bizId, snapshot))
                .orElse(current);
    }

    /**
     * 查询支付会话。
     *
     * @param bizId 业务号
     * @return 支付会话
     */
    public PaymentSession get(String bizId) {
        return require(bizId);
    }

    /**
     * 返回全部会话供定时对账。
     *
     * @return 会话集合
     */
    public Collection<PaymentSession> all() {
        return repository.findAll();
    }

    /** 按业务号读取会话，不存在时抛出调用错误。 */
    private PaymentSession require(String bizId) {
        return repository.findByBizId(bizId)
                .orElseThrow(() -> new IllegalArgumentException("payment session not found: " + bizId));
    }
}
