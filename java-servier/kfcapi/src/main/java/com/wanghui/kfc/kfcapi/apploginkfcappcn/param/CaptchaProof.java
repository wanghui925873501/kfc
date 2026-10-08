package com.wanghui.kfc.kfcapi.apploginkfcappcn.param;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.ToString;

/** 同一登录挑战成功后用于一次 {@code /svc/to} 请求的验证字段。 */
@Data
public class CaptchaProof {
    /** 供调用方逐项赋值。 */
    public CaptchaProof() { }

    /** APK 中的验证类型，当前为滑块 1 或图文 2。 */
    private int rt;
    /** 首次上游风险响应携带的事件标识。 */
    @ToString.Exclude
    private String eventId;
    /** {@code startCaptcha} 返回的用户标识。 */
    @ToString.Exclude
    private String userid;
    /** {@code startCaptcha} 返回的极验服务状态。 */
    private String gtServerStatus;
    /** 识别服务返回的最终挑战标识。 */
    @ToString.Exclude
    private String gtChallenge;
    /** 极验验证结果。 */
    @ToString.Exclude
    private String gtValidate;
    /** 极验二次校验值。 */
    @ToString.Exclude
    private String gtSeccode;

    /**
     * 拒绝缺失或混用的挑战字段。
     * @throws IllegalArgumentException 验证类型或字段不完整时
     */
    public void requireComplete() {
        if ((rt != 1 && rt != 2) || StrUtil.hasBlank(eventId, userid, gtServerStatus,
                gtChallenge, gtValidate, gtSeccode)) {
            throw new IllegalArgumentException("验证码事件或验证结果不完整");
        }
    }
}
