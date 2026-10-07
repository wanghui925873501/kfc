package com.wanghui.kfc.alipay.infrastructure;

import com.wanghui.kfc.alipay.application.KfcOrderSnapshot;
import com.wanghui.kfc.alipay.application.KfcOrderStatusClient;
import com.wanghui.kfc.alipay.config.KfcAliPayProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 调用 {@code java-servier} 内部订单接口的 HTTP 适配器。
 */
@Component
public class HttpKfcOrderStatusClient implements KfcOrderStatusClient {

    /** 支付编排配置。 */
    private final KfcAliPayProperties properties;

    /** 只用于调用自有后端的 HTTP 客户端。 */
    private final RestClient restClient;

    /**
     * 创建订单查询适配器。
     *
     * @param properties 支付编排配置
     * @param builder Spring HTTP 客户端构造器
     */
    public HttpKfcOrderStatusClient(KfcAliPayProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<KfcOrderSnapshot> query(String orderId) {
        KfcAliPayProperties.Reconciliation reconciliation = properties.reconciliation();
        if (reconciliation == null || !reconciliation.enabled()) {
            return Optional.empty();
        }
        String path = reconciliation.pathTemplate().replace("{orderId}", orderId);
        try {
            ResponseEntity<KfcOrderSnapshot> response = restClient.get()
                    .uri(reconciliation.baseUrl() + path)
                    .retrieve()
                    .toEntity(KfcOrderSnapshot.class);
            return Optional.ofNullable(response.getBody());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
