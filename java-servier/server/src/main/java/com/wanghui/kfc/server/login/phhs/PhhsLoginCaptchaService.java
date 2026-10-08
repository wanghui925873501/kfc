package com.wanghui.kfc.server.login.phhs;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Client;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Result;
import com.wanghui.kfc.phhsapi.apploginphdappcn.api.PhhsLoginApi;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsCaptchaProof;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsSendSmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsStartCaptchaVo;
import org.springframework.stereotype.Service;

/** 为 PHHS 短信登录编排一次小辉极验三代挑战和最多一次上游重试。 */
@Service
public class PhhsLoginCaptchaService {
    /** PHHS App 统一登录接口。 */
    private final PhhsLoginApi loginApi;
    /** 用户本机部署的小辉极验三代客户端。 */
    private final Captcha3Client captchaClient;

    /**
     * 创建 PHHS 图形验证编排服务。
     * @param loginApi PHHS App 登录接口
     * @param captchaClient 跨品牌共用的本机小辉版客户端
     */
    public PhhsLoginCaptchaService(PhhsLoginApi loginApi, Captcha3Client captchaClient) {
        this.loginApi = loginApi;
        this.captchaClient = captchaClient;
    }

    /**
     * 在调用方的短信单次锁内发码；遇到 PHHS 风险码时只补发一次。
     * @param param 同一手机号及 PHHS 安装上下文
     * @return 最终 PHHS 业务结果
     */
    public PhhsSendSmsCodeVo sendSmsCode(PhhsSendSmsCodeParam param) {
        PhhsSendSmsCodeVo first = loginApi.sendSmsCode(param);
        if (first == null || !first.requiresHumanVerification()) return first;
        PhhsCaptchaProof proof = solve(first.getErrCode(), first.getErrData(), param.getContext());
        return loginApi.sendSmsCodeVerified(param, proof);
    }

    /**
     * 在调用方的登录单次锁内登录；遇到 PHHS 风险码时只补登一次。
     * @param param 同一手机号、短信码及 PHHS 安装上下文
     * @return 最终 PHHS 登录结果
     */
    public PhhsLoginBySmsCodeVo loginBySmsCode(PhhsLoginBySmsCodeParam param) {
        PhhsLoginBySmsCodeVo first = loginApi.loginBySmsCode(param);
        if (first == null || !first.requiresVerification()) return first;
        PhhsCaptchaProof proof = solve(first.getErrCode(), first.getErrData(), param.getContext());
        return loginApi.loginBySmsCodeVerified(param, proof);
    }

    private PhhsCaptchaProof solve(
            Integer errorCode, JsonNode errData, PhhsLoginContext context) {
        int rt = Integer.valueOf(5910060).equals(errorCode) ? 1 : 2;
        String eventId = errData == null ? "" : errData.path("data").asText("");
        if (StrUtil.isBlank(eventId)) {
            throw new IllegalStateException("PHHS 上游挑战缺少事件标识");
        }
        PhhsStartCaptchaVo registration = loginApi.startCaptcha(rt, context);
        JsonNode data = registration == null ? null : registration.getData();
        if (registration == null || registration.getErrCode() == null
                || registration.getErrCode() != 0 || data == null
                || StrUtil.hasBlank(data.path("gt").asText(""),
                        data.path("challenge").asText(""), data.path("userid").asText(""),
                        data.path("gtServerStatus").asText(""))) {
            throw new IllegalStateException("PHHS 上游验证注册数据不完整");
        }
        Captcha3Result result = captchaClient.solve(
                data.path("gt").asText(), data.path("challenge").asText());
        PhhsCaptchaProof proof = new PhhsCaptchaProof();
        proof.setRt(rt);
        proof.setEventId(eventId);
        proof.setUserid(data.path("userid").asText());
        proof.setGtServerStatus(data.path("gtServerStatus").asText());
        proof.setGtChallenge(result.getChallenge());
        proof.setGtValidate(result.getValidate());
        proof.setGtSeccode(result.getSeccode());
        proof.requireComplete();
        return proof;
    }
}
