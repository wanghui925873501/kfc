package com.wanghui.kfc.server.controller.login.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 前端请求发送短信验证码时提交的参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "SendSmsCodeParam", description = "手机号短信验证码发送参数")
public class SendSmsCodeParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public SendSmsCodeParam() { }
}
