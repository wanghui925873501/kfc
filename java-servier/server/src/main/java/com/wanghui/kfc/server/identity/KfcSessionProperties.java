package com.wanghui.kfc.server.identity;

import java.time.Duration;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地登录会话的有效期与静态加密配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.session")
public class KfcSessionProperties {
    /** 供 Spring 绑定会话有效期与密钥。 */
    public KfcSessionProperties() { }

    /** 本地会话有效期，不代表上游 token 的有效期。 */
    private Duration ttl = Duration.ofHours(8);
    /** 忽略文件或环境变量提供的 256 位 AES 密钥。 */
    @ToString.Exclude
    private String keyBase64;
}
