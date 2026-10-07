package com.wanghui.kfc.alipay.application;

import com.wanghui.kfc.alipay.config.KfcAliPayProperties;
import com.wanghui.kfc.alipay.domain.PaymentSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 对非终态支付会话执行有界、幂等的 KFC 服务端状态查询。
 */
@Component
public class PaymentReconciliationJob {

    /** 支付编排服务。 */
    private final PaymentOrchestrationService service;

    /** 查询开关。 */
    private final KfcAliPayProperties properties;

    /**
     * 创建定时对账任务。
     *
     * @param service 支付编排服务
     * @param properties 支付配置
     */
    public PaymentReconciliationJob(
            PaymentOrchestrationService service,
            KfcAliPayProperties properties
    ) {
        this.service = service;
        this.properties = properties;
    }

    /**
     * 周期性刷新已经交给客户端且尚未终结的支付会话。
     */
    @Scheduled(fixedDelayString = "${kfc-ali-pay.reconciliation.interval-ms:2000}")
    public void reconcilePendingSessions() {
        if (properties.reconciliation() == null || !properties.reconciliation().enabled()) {
            return;
        }
        for (PaymentSession session : service.all()) {
            if (!session.terminal() && session.status() != null) {
                service.reconcileNow(session.bizId());
            }
        }
    }
}
