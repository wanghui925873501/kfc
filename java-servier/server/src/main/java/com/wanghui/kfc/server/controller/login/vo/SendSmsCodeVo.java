package com.wanghui.kfc.server.controller.login.vo;

import com.wanghui.kfc.server.controller.login.dto.LoginNextAction;
import com.wanghui.kfc.server.controller.login.dto.LoginSessionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 短信发送接口返回的脱敏结果。 */
@Data
@Schema(name = "SendSmsCodeVo", description = "短信发送或已有登录态复用结果")
public class SendSmsCodeVo {
    /** 供 Controller 逐项设置返回字段。 */
    public SendSmsCodeVo() { }

    /** 请求结束时是否已经取得本地登录会话。 */
    @Schema(description = "是否已经取得本地登录会话", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean loggedIn;
    /** 本次请求是否实际确认发送了短信。 */
    @Schema(description = "本次请求是否实际确认发送了短信", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean smsSent;
    /** 前端下一步应填写验证码或直接使用会话。 */
    @Schema(description = "前端下一步操作", accessMode = Schema.AccessMode.READ_ONLY)
    private LoginNextAction nextAction;
    /** 已有 token 时签发的本地会话；实际发码时为空。 */
    @Schema(description = "已有登录态时返回的本地会话；实际发码时为空",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LoginSessionDto session;
}
