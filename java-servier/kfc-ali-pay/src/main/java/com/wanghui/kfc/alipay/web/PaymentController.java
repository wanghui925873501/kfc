package com.wanghui.kfc.alipay.web;

import com.wanghui.kfc.alipay.application.KfcOrderSnapshot;
import com.wanghui.kfc.alipay.application.PaymentOrchestrationService;
import com.wanghui.kfc.alipay.config.KfcAliPayProperties;
import com.wanghui.kfc.alipay.domain.PaymentSession;
import com.wanghui.kfc.alipay.infrastructure.CdpBrowserHandoffClient;
import com.wanghui.kfc.alipay.web.dto.BeginPaymentRequest;
import com.wanghui.kfc.alipay.web.dto.ClientPaymentResultRequest;
import com.wanghui.kfc.alipay.web.dto.PaymentHandoffResponse;
import com.wanghui.kfc.alipay.web.dto.PaymentStatusResponse;
import com.wanghui.kfc.alipay.web.dto.ReconcilePaymentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * 暴露 UniApp 支付交接、客户端提示和服务端对账接口。
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    /** 支付编排服务。 */
    private final PaymentOrchestrationService service;

    /** CDP 浏览器人工交接客户端。 */
    private final CdpBrowserHandoffClient cdpClient;

    /** 管理与内部接口配置。 */
    private final KfcAliPayProperties properties;

    /**
     * 创建支付接口控制器。
     *
     * @param service 支付编排服务
     * @param cdpClient CDP 人工交接客户端
     * @param properties 服务配置
     */
    public PaymentController(
            PaymentOrchestrationService service,
            CdpBrowserHandoffClient cdpClient,
            KfcAliPayProperties properties
    ) {
        this.service = service;
        this.cdpClient = cdpClient;
        this.properties = properties;
    }

    /**
     * 创建幂等支付会话并返回 UniApp 支付数据。
     *
     * @param request KFC 收银台生成的支付信息
     * @return UniApp 支付交接数据
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentHandoffResponse begin(@Valid @RequestBody BeginPaymentRequest request) {
        PaymentSession session = service.create(
                request.getBizId(),
                request.getOrderId(),
                request.getAmountFen(),
                request.getPaymentContent(),
                request.getPaymentUrl()
        );
        return PaymentHandoffResponse.from(session);
    }

    /**
     * 标记 UniApp 已经调用 {@code uni.requestPayment}。
     *
     * @param bizId 业务号
     * @return 当前账本
     */
    @PostMapping("/{bizId}/launched")
    public PaymentStatusResponse launched(@PathVariable("bizId") String bizId) {
        return PaymentStatusResponse.from(service.markLaunched(bizId));
    }

    /**
     * 接收客户端支付提示；该接口不会直接把订单置为已支付。
     *
     * @param bizId 业务号
     * @param request 客户端结果
     * @return 等待服务端确认的账本
     */
    @PostMapping("/{bizId}/client-result")
    public PaymentStatusResponse clientResult(
            @PathVariable("bizId") String bizId,
            @Valid @RequestBody ClientPaymentResultRequest request
    ) {
        return PaymentStatusResponse.from(
                service.acceptClientOutcome(bizId, request.getOutcome(), request.getMessage())
        );
    }

    /**
     * 查询支付会话并按需主动刷新 KFC 订单状态。
     *
     * @param bizId 业务号
     * @param refresh 是否立刻查询自有 KFC 后端
     * @return 当前支付账本
     */
    @GetMapping("/{bizId}")
    public PaymentStatusResponse get(
            @PathVariable("bizId") String bizId,
            @org.springframework.web.bind.annotation.RequestParam(name = "refresh", defaultValue = "false")
            boolean refresh
    ) {
        PaymentSession session = refresh ? service.reconcileNow(bizId) : service.get(bizId);
        return PaymentStatusResponse.from(session);
    }

    /**
     * 由可信内部服务写入 KFC 订单查询结果。
     *
     * @param bizId 业务号
     * @param token 内部管理令牌
     * @param request KFC 订单状态
     * @return 更新后的支付账本
     */
    @PostMapping("/{bizId}/reconcile")
    public PaymentStatusResponse reconcile(
            @PathVariable("bizId") String bizId,
            @RequestHeader("X-Kfc-Ali-Pay-Token") String token,
            @Valid @RequestBody ReconcilePaymentRequest request
    ) {
        if (properties.managementToken() == null
                || properties.managementToken().isBlank()
                || !properties.managementToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid management token");
        }
        return PaymentStatusResponse.from(service.reconcile(
                bizId,
                new KfcOrderSnapshot(request.getOrderId(), request.getStatus(), request.getStatusName())
        ));
    }

    /**
     * 在 Chromium 中打开 H5 支付链接供用户本人操作。
     *
     * @param bizId 业务号
     * @return CDP target 标识
     * @throws IOException CDP 请求失败
     * @throws InterruptedException CDP 请求被中断
     */
    @PostMapping("/{bizId}/browser-handoff")
    public java.util.Map<String, String> browserHandoff(@PathVariable("bizId") String bizId)
            throws IOException, InterruptedException {
        PaymentSession session = service.get(bizId);
        if (session.paymentUrl() == null || session.paymentUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "paymentUrl is unavailable");
        }
        String targetId = cdpClient.open(session.paymentUrl());
        service.markLaunched(bizId);
        return java.util.Map.of("targetId", targetId, "mode", "human-handoff");
    }
}
