package com.wanghui.kfc.server.web.logging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 配置内部 API 请求响应日志的开关与单个报文最大输出长度。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.http-log")
public class ApiHttpLogProperties {
    /** 供 Spring 绑定 HTTP 日志配置。 */
    public ApiHttpLogProperties() { }

    /** 是否记录经过 Controller 的 API 请求与响应。 */
    private boolean enabled = true;
    /** 脱敏后单个请求体、响应体或查询参数允许输出的最大字符数。 */
    private int maxBodyLength = 16384;
}
