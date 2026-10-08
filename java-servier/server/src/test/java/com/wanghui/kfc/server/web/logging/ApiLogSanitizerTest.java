package com.wanghui.kfc.server.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** 验证 API 日志对请求和响应中的敏感值执行统一脱敏。 */
class ApiLogSanitizerTest {
    /** 被测试的日志脱敏器。 */
    private final ApiLogSanitizer sanitizer = new ApiLogSanitizer();

    /** 手机号保留定位所需片段，验证码、token 与设备标识不可出现在日志中。 */
    @Test
    void sanitizesSensitiveJsonFields() {
        String sanitized = sanitizer.sanitizeJson("""
                {"phone":"13800000000","smsCode":"012345","flowId":"flow-value",
                "data":{"token":"token-value","installationId":"install-value"},"code":"OK"}
                """, 16384);

        assertThat(sanitized).contains("138****0000", "\"code\":\"OK\"");
        assertThat(sanitized).doesNotContain("13800000000", "012345", "flow-value",
                "token-value", "install-value");
    }

    /** 查询参数中的手机号和敏感标识按同一规则脱敏。 */
    @Test
    void sanitizesQueryParameters() {
        String sanitized = sanitizer.sanitizeParameters(Map.of(
                "phone", new String[] {"13800000000"},
                "sessionId", new String[] {"session-value"}), 16384);

        assertThat(sanitized).contains("138****0000");
        assertThat(sanitized).doesNotContain("13800000000", "session-value");
    }
}
