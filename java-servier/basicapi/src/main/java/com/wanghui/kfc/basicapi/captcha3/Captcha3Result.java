package com.wanghui.kfc.basicapi.captcha3;

import lombok.Data;
import lombok.ToString;

/** 本机小辉极验三代服务返回的最小验证结果。 */
@Data
public class Captcha3Result {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public Captcha3Result() { }

    /** 识别服务返回的验证类型。 */
    private String type;
    /** 极验最终挑战标识。 */
    @ToString.Exclude
    private String challenge;
    /** 极验验证票据。 */
    @ToString.Exclude
    private String validate;
    /** 极验二次校验值。 */
    @ToString.Exclude
    private String seccode;
}
