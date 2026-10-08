package com.wanghui.kfc.server.login;

import lombok.Data;
import lombok.ToString;

/** 登录业务层返回给 Controller 的脱敏结果，不包含任何 KFC 上游 token。 */
@Data
public class KfcLoginResult {
    /** 供业务服务逐项设置结果。 */
    public KfcLoginResult() { }

    /** 请求结束时是否已有可用的本地登录会话。 */
    private boolean loggedIn;
    /** 本次调用是否实际确认发送了短信。 */
    private boolean smsSent;
    /** 本次调用是否复用了数据库中已有的上游 token。 */
    private boolean reusedStoredToken;
    /** 返回给前端的本地随机会话标识。 */
    @ToString.Exclude
    private String sessionId;
    /** 本地会话绑定的安装标识。 */
    @ToString.Exclude
    private String installationId;
    /** 本地会话有效秒数，不代表 KFC token 有效期。 */
    private long expiresInSeconds;
}
