package com.wanghui.kfc.alipay.application;

import com.wanghui.kfc.alipay.domain.ClientPaymentOutcome;
import com.wanghui.kfc.alipay.domain.PaymentSession;
import com.wanghui.kfc.alipay.domain.PaymentStatus;
import com.wanghui.kfc.alipay.repository.InMemoryPaymentSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证支付安全边界：客户端成功提示不能替代 KFC 服务端订单状态。
 */
class PaymentOrchestrationServiceTest {

    /** 被测支付编排服务。 */
    private PaymentOrchestrationService service;

    /**
     * 每个测试使用全新的内存账本。
     */
    @BeforeEach
    void setUp() {
        service = new PaymentOrchestrationService(
                new InMemoryPaymentSessionRepository(),
                orderId -> Optional.empty()
        );
    }

    /**
     * 客户端报告成功后仍应等待 KFC 服务端确认。
     */
    @Test
    void clientSuccessDoesNotMarkPaymentPaid() {
        service.create("biz-1", "order-1", 700, "signed-order", null);

        PaymentSession result = service.acceptClientOutcome(
                "biz-1", ClientPaymentOutcome.SUCCESS, "client returned success"
        );

        assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMING);
    }

    /**
     * KFC 状态 102 才能确认付款后的预约订单。
     */
    @Test
    void kfcReservedStatusMarksPaymentPaid() {
        service.create("biz-2", "order-2", 700, "signed-order", null);

        PaymentSession result = service.reconcile(
                "biz-2", new KfcOrderSnapshot("order-2", "102", "预约成功")
        );

        assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.kfcOrderStatus()).isEqualTo("102");
    }

    /**
     * KFC 状态 504 应把账本更新为已取消。
     */
    @Test
    void kfcCancelledStatusMarksPaymentCancelled() {
        service.create("biz-3", "order-3", 700, "signed-order", null);

        PaymentSession result = service.reconcile(
                "biz-3", new KfcOrderSnapshot("order-3", "504", "已取消")
        );

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    /**
     * 同一个幂等号不得绑定到另一笔订单。
     */
    @Test
    void duplicateBizIdRejectsDifferentOrder() {
        service.create("biz-4", "order-4", 700, "signed-order", null);

        assertThatThrownBy(() -> service.create(
                "biz-4", "order-other", 800, "other-signed-order", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 内部对账结果必须属于同一 KFC 订单。
     */
    @Test
    void reconcileRejectsAnotherOrder() {
        service.create("biz-5", "order-5", 700, "signed-order", null);

        assertThatThrownBy(() -> service.reconcile(
                "biz-5", new KfcOrderSnapshot("order-other", "102", "预约成功")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
