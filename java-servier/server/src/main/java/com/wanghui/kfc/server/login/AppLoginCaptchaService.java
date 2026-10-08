package com.wanghui.kfc.server.login;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Client;
import com.wanghui.kfc.basicapi.captcha3.Captcha3Result;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.api.AppLoginApi;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.CaptchaProof;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.LoginBySmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.StartCaptchaVo;
import org.springframework.stereotype.Service;

/** 在同一调用内处理一次登录挑战，并限制补发或补登请求至多一次。 */
@Service
public class AppLoginCaptchaService {
    /** KFC App 统一登录接口。 */
    private final AppLoginApi loginApi;
    /** 用户部署的本机图形验证接口。 */
    private final Captcha3Client captchaClient;

    /**
     * 创建短信登录验证编排服务。
     * @param loginApi KFC App 登录接口
     * @param captchaClient 用户自己的本地识别客户端
     */
    public AppLoginCaptchaService(AppLoginApi loginApi, Captcha3Client captchaClient) {
        this.loginApi = loginApi;
        this.captchaClient = captchaClient;
    }

    /**
     * 在调用方的短信单次触发锁内发码；仅遇指定挑战时补发一次。
     * @param param 同一手机号及安装上下文
     * @return 最终上游业务结果，不把验证结果误当短信发送成功
     */
    public SendSmsCodeVo sendSmsCode(SendSmsCodeParam param) {
        SendSmsCodeVo first = loginApi.sendSmsCode(param);
        if (first == null || !first.requiresHumanVerification()) return first;
        CaptchaProof proof = solve(first.getErrCode(), first.getErrData(), param.getContext());
        return loginApi.sendSmsCodeVerified(param, proof);
    }

    /**
     * 在调用方的登录单次触发锁内登录；仅遇指定挑战时补登一次。
     * @param param 同一手机号、短信码及安装上下文
     * @return 最终上游业务结果，仅成功结果可交给身份服务持久化
     */
    public LoginBySmsCodeVo loginBySmsCode(LoginBySmsCodeParam param) {
        LoginBySmsCodeVo first = loginApi.loginBySmsCode(param);
        if (first == null || !first.requiresVerification()) return first;
        CaptchaProof proof = solve(first.getErrCode(), first.getErrData(), param.getContext());
        return loginApi.loginBySmsCodeVerified(param, proof);
    }

    private CaptchaProof solve(Integer errorCode, JsonNode errData, AppLoginContext context) {
        int rt = Integer.valueOf(5910060).equals(errorCode) ? 1 : 2;
        String eventId = errData == null ? "" : errData.path("data").asText("");
        if (StrUtil.isBlank(eventId)) throw new IllegalStateException("上游挑战缺少事件标识");
        StartCaptchaVo registration = loginApi.startCaptcha(rt, context);
        JsonNode data = registration == null ? null : registration.getData();
        if (registration == null || registration.getErrCode() == null || registration.getErrCode() != 0
                || data == null || StrUtil.hasBlank(data.path("gt").asText(""),
                        data.path("challenge").asText(""), data.path("userid").asText(""),
                        data.path("gtServerStatus").asText(""))) {
            throw new IllegalStateException("上游验证注册数据不完整");
        }
        Captcha3Result result = captchaClient.solve(data.path("gt").asText(),
                data.path("challenge").asText());
        CaptchaProof proof = new CaptchaProof();
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
