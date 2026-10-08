package com.wanghui.kfc.phhsapi.apploginphdappcn.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 保存一次必胜客短信验证码登录所需的敏感输入和真实客户端上下文。 */
@Data
@AllArgsConstructor
public class PhhsLoginBySmsCodeParam {
    /** 供配置框架或调用方逐项设置字段。 */
    public PhhsLoginBySmsCodeParam() { }

    /** 用户授权测试的明文手机号，仅在 Java 进程内使用。 */
    @ToString.Exclude
    private String phone;
    /** 用户本人收到的六位短信验证码，不持久化。 */
    @ToString.Exclude
    private String smsCode;
    /** 与发码阶段相同安装的真实客户端上下文。 */
    private PhhsLoginContext context;

    /** 校验登录参数，避免缺失或格式错误的验证码触发上游请求。 */
    public void requireComplete() {
        if (phone == null || !phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("phone must be an 11-digit mobile number");
        }
        if (smsCode == null || !smsCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("smsCode must contain six digits");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        context.requireComplete();
    }
}
