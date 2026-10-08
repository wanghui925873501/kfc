package com.wanghui.kfc.server.controller.login.vo;

import com.wanghui.kfc.server.controller.login.dto.LoginSessionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 验证码登录接口返回的本地会话结果。 */
@Data
@Schema(name = "SmsCodeLoginVo", description = "短信验证码登录后的本地会话结果")
public class SmsCodeLoginVo {
    /** 供 Controller 逐项设置返回字段。 */
    public SmsCodeLoginVo() { }

    /** 是否没有请求 KFC，而是复用了数据库中的已有 token。 */
    @Schema(description = "是否复用了数据库中已有的 KFC token", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean reusedStoredToken;
    /** 登录成功后签发的本地会话。 */
    @Schema(description = "登录成功后签发的本地会话", accessMode = Schema.AccessMode.READ_ONLY)
    private LoginSessionDto session;
}
