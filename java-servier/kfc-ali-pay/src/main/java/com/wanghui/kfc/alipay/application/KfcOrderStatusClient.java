package com.wanghui.kfc.alipay.application;

import java.util.Optional;

/**
 * 通过自有 Java 后端查询 KFC 服务端订单状态，避免信任客户端支付回调。
 */
public interface KfcOrderStatusClient {

    /**
     * 查询一笔 KFC 订单。
     *
     * @param orderId KFC 订单号
     * @return 查询成功时的状态快照
     */
    Optional<KfcOrderSnapshot> query(String orderId);
}
