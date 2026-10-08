package com.wanghui.kfc.server.rnorder;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 配置 RN 点餐流程会话在 Redis 中的有效期。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.rn-order")
public class RnOrderFlowProperties {
    /** 供 Spring 绑定 RN 点餐流程配置。 */
    public RnOrderFlowProperties() { }

    /** 前端一次选店到商品详情流程的最大空闲时间。 */
    private Duration flowTtl = Duration.ofMinutes(15);
}
