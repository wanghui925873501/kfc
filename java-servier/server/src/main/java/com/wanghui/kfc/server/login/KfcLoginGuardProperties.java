package com.wanghui.kfc.server.login;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 配置登录外部请求的互斥时间和重复触发冷却时间。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.login-guard")
public class KfcLoginGuardProperties {
    /** 供 Spring 绑定登录触发保护配置。 */
    public KfcLoginGuardProperties() { }

    /** 同一手机号一次完整登录编排的最大互斥时间。 */
    private Duration mutexTtl = Duration.ofMinutes(3);
    /** 同一手机号成功取得发码资格后的冷却时间。 */
    private Duration smsCooldown = Duration.ofMinutes(1);
    /** 同一手机号验证码登录请求的短期防重复时间。 */
    private Duration loginCooldown = Duration.ofSeconds(10);
}
