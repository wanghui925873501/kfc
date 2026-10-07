package com.wanghui.kfc.alipay.repository;

import com.wanghui.kfc.alipay.domain.PaymentSession;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 开发阶段使用的线程安全内存账本；生产环境应替换为带唯一索引的数据库实现。
 */
@Repository
public class InMemoryPaymentSessionRepository implements PaymentSessionRepository {

    /** 按 {@code bizId} 保存的进程内账本。 */
    private final ConcurrentMap<String, PaymentSession> sessions = new ConcurrentHashMap<>();

    /**
     * 创建空的进程内支付会话仓库。
     */
    public InMemoryPaymentSessionRepository() {
        // 使用字段初始化器创建线程安全账本。
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PaymentSession> findByBizId(String bizId) {
        return Optional.ofNullable(sessions.get(bizId));
    }

    /** {@inheritDoc} */
    @Override
    public PaymentSession save(PaymentSession session) {
        sessions.put(session.bizId(), session);
        return session;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<PaymentSession> findAll() {
        return List.copyOf(sessions.values());
    }
}
