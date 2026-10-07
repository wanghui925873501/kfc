package com.wanghui.kfc.kfcapi.apploginkfcappcn.param;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 短信验证码登录所需的业务输入；手机号和验证码在上游调用前加密。 */
@Data
@AllArgsConstructor
public class LoginBySmsCodeParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public LoginBySmsCodeParam() { }

    /** 授权测试账号的明文手机号，仅在后端处理。 */
    @ToString.Exclude
    private String phone;
    /** 用户本人提供的验证码，仅在本次调用内使用。 */
    @ToString.Exclude
    private String smsCode;
    /** 当前应用实际设备及客户端上下文。 */
    private AppLoginContext context;

    /** 校验验证码登录所需的输入。 */
    public void requireComplete() {
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(smsCode, "smsCode");
        Objects.requireNonNull(context, "context").requireComplete();
    }
}
