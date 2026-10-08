package com.wanghui.kfc.server.controller.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 返回给前端的本地登录会话，不包含 KFC 上游 token。 */
@Data
@AllArgsConstructor
@Schema(name = "LoginSession", description = "后端签发的本地登录会话")
public class LoginSessionDto {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public LoginSessionDto() { }

    /** 前端后续请求携带的随机本地会话标识。 */
    @ToString.Exclude
    @Schema(description = "随机本地会话标识；不是 KFC token", accessMode = Schema.AccessMode.READ_ONLY)
    private String sessionId;
    /** 会话绑定的后端安装标识。 */
    @ToString.Exclude
    @Schema(description = "会话绑定的安装标识", accessMode = Schema.AccessMode.READ_ONLY)
    private String installationId;
    /** 本地 Redis 会话的有效秒数。 */
    @Schema(description = "本地会话有效秒数；不代表 KFC token 有效期", accessMode = Schema.AccessMode.READ_ONLY)
    private long expiresInSeconds;
}
