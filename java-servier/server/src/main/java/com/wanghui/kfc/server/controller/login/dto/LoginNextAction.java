package com.wanghui.kfc.server.controller.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 指示前端在短信发送接口完成后的下一步操作。 */
@Schema(description = "短信发送接口完成后的下一步操作")
public enum LoginNextAction {
    /** 尚未登录，需要用户填写本次收到的短信验证码。 */
    ENTER_SMS_CODE,
    /** 数据库已有 token，前端应直接使用返回的本地会话。 */
    USE_SESSION
}
