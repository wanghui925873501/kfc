package com.wanghui.kfc.alipay.repository;

import com.wanghui.kfc.alipay.domain.PaymentSession;

import java.util.Collection;
import java.util.Optional;

/**
 * 支付账本存储抽象。
 */
public interface PaymentSessionRepository {

    /**
     * 按业务号查询账本。
     *
     * @param bizId 幂等业务号
     * @return 账本
     */
    Optional<PaymentSession> findByBizId(String bizId);

    /**
     * 保存支付账本。
     *
     * @param session 新快照
     * @return 保存后的快照
     */
    PaymentSession save(PaymentSession session);

    /**
     * 返回当前全部账本，用于开发阶段的定时对账。
     *
     * @return 账本快照集合
     */
    Collection<PaymentSession> findAll();
}
