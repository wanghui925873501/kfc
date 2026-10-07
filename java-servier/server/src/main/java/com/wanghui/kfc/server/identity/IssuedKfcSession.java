package com.wanghui.kfc.server.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 返回给客户端的本地会话句柄，不包含上游登录票据。 */
@Data
@AllArgsConstructor
public class IssuedKfcSession {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public IssuedKfcSession() { }

    /** 本应用生成的不可猜测会话标识。 */
    @ToString.Exclude
    private String sessionId;
    /** 已关联的 KFC 用户映射主键。 */
    private Long kfcUserId;
}
