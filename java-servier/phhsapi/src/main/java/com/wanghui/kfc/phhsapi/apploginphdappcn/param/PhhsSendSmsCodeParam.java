package com.wanghui.kfc.phhsapi.apploginphdappcn.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 保存一次必胜客短信验证码发送所需的授权手机号和真实客户端上下文。 */
@Data
@AllArgsConstructor
public class PhhsSendSmsCodeParam {
    /** 供配置框架或调用方逐项设置字段。 */
    public PhhsSendSmsCodeParam() { }

    /** 用户授权测试的明文手机号，仅在 Java 进程内使用。 */
    @ToString.Exclude
    private String phone;
    /** 同一安装的真实客户端上下文。 */
    private PhhsLoginContext context;

    /** 校验发码参数，避免无效手机号触发上游请求。 */
    public void requireComplete() {
        if (phone == null || !phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("phone must be an 11-digit mobile number");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        context.requireComplete();
    }
}
