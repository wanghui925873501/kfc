package com.wanghui.kfc.kfcapi.apploginkfcappcn.vo;

import lombok.Data;
import lombok.ToString;

/** 本机小辉极验三代服务返回的有效在线挑战结果。 */
@Data
public class Captcha3ResultVo {
    /** 供调用方逐项赋值。 */
    public Captcha3ResultVo() { }

    /** 本机服务声明的挑战类型。 */
    private String type;
    /** 可能经服务刷新后的最终 challenge。 */
    @ToString.Exclude
    private String challenge;
    /** 极验服务确认的 validate。 */
    @ToString.Exclude
    private String validate;
    /** 服务直接返回时使用的 seccode；缺失时按三代约定生成。 */
    @ToString.Exclude
    private String seccode;
}
