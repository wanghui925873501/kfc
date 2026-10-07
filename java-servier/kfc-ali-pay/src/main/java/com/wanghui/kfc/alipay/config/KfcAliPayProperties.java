package com.wanghui.kfc.alipay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 支付编排、KFC 订单对账和可选 CDP 人工交接配置。
 *
 * @param managementToken 内部状态确认接口令牌
 * @param reconciliation KFC 订单状态查询配置
 * @param cdp Chromium CDP 人工交接配置
 */
@ConfigurationProperties("kfc-ali-pay")
public record KfcAliPayProperties(
        String managementToken,
        Reconciliation reconciliation,
        Cdp cdp
) {

    /**
     * KFC 订单状态查询配置。
     *
     * @param enabled 是否启用定时查询
     * @param baseUrl KFC 自有后端地址
     * @param pathTemplate 带 {@code {orderId}} 的订单查询路径
     * @param intervalMs 定时查询间隔
     */
    public record Reconciliation(
            boolean enabled,
            String baseUrl,
            String pathTemplate,
            long intervalMs
    ) {
    }

    /**
     * Chromium CDP 人工交接配置。
     *
     * @param enabled 是否允许创建浏览器付款页
     * @param debugAddress Chromium 远程调试地址
     * @param allowedHosts 允许打开的支付宝官方域名
     */
    public record Cdp(
            boolean enabled,
            String debugAddress,
            List<String> allowedHosts
    ) {
    }
}
