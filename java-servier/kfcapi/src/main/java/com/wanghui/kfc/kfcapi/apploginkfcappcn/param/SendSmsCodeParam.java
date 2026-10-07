package com.wanghui.kfc.kfcapi.apploginkfcappcn.param;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 发送短信验证码所需的业务输入；手机号在上游调用前加密。 */
@Data
@AllArgsConstructor
public class SendSmsCodeParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public SendSmsCodeParam() { }

    /** 授权测试账号的明文手机号，仅在后端处理。 */
    @ToString.Exclude
    private String phone;
    /** 当前应用实际设备及客户端上下文。 */
    private AppLoginContext context;

    /** 校验短信请求所需的输入。 */
    public void requireComplete() {
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(context, "context").requireComplete();
    }
}
