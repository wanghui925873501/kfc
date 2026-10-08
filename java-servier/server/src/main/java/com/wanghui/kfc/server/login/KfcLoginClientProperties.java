package com.wanghui.kfc.server.login;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 保存后端登录调用使用的固定客户端档案，不接受前端任意覆盖上游请求头。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.login-client")
public class KfcLoginClientProperties {
    /** 供 Spring 绑定登录客户端档案。 */
    public KfcLoginClientProperties() { }

    /** 登录上下文使用的城市编码。 */
    private String cityCode = "310000";
    /** 与当前协议证据对应的客户端版本。 */
    private String appVersion = "6.37.0";
    /** 与当前客户端版本配套的 User-Agent。 */
    private String userAgent = "Dalvik/2.1.0 (Linux; U; Android 9; Local Test Device Build/LOCAL) "
            + "SuperKFC Mobile Android Client KFCSuperAPP v6.37.0";
}
