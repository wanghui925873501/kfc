package com.wanghui.kfc.server.controller.login.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端使用手机号和当次短信验证码登录时提交的参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "SmsCodeLoginParam", description = "手机号短信验证码登录参数")
public class SmsCodeLoginParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public SmsCodeLoginParam() { }
    /** 用户当次收到的六位短信验证码，只参与本次调用且不持久化。 */
    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "[0-9]{6}", message = "短信验证码必须是六位数字")
    @ToString.Exclude
    @Schema(description = "当次收到的六位短信验证码，不持久化",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6, maxLength = 6)
    private String smsCode;
}
