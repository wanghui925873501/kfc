package com.wanghui.kfc.kfcapi.apploginkfcappcn.api;

import com.wanghui.kfc.kfcapi.KfcUpstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wanghui.kfc.basicapi.Upstream;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.CaptchaProof;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.LoginBySmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.StartCaptchaVo;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 按真实 Reqable 证据封装 {@code applogin.kfcapp.cn} 的 App 短信登录接口。 */
@Component
public class AppLoginApi {
    /** 受固定域名和路径限制的 HTTP 传输层。 */
    private final UpstreamGateway gateway;
    /** 登录敏感字段加密器。 */
    private final AppLoginCrypto crypto;
    /** 本机请求体配置。 */
    private final AppLoginProperties properties;
    /** 只用于将响应 JSON 解析成内部返回类型。 */
    private final ObjectMapper mapper;

    /**
     * 创建已抓包验证的 App 登录客户端。
     *
     * @param gateway 固定域名与路径的传输层
     * @param crypto 登录字段加密器
     * @param properties 本机请求体配置
     * @param mapper JSON 响应解析器
     */
    public AppLoginApi(UpstreamGateway gateway, AppLoginCrypto crypto,
                       AppLoginProperties properties, ObjectMapper mapper) {
        this.gateway = gateway;
        this.crypto = crypto;
        this.properties = properties;
        this.mapper = mapper;
    }

    /**
     * 调用 {@code applogin.kfcapp.cn} 的 {@code POST /api/user/sendSmsCode}。
     * APK/Reqable 证据：{@code 抓包文件/登录/01-[6224]-send-sms-code} 与
     * {@code 抓包文件/登录/03-[8600]-send-sms-code-20261007}；后者返回 {@code errCode=0}。
     * 不同设备及风控 SDK 上下文的适用性仍待验证。
     * 调用会真实发送短信，必须由上层执行当次确认、限流和单次触发锁。
     *
     * @param param 授权手机号和真实客户端上下文
     * @return 上游响应概要，字段结构仍待补充验证
     * @throws IllegalStateException 必需的本机配置缺失或响应无法解析时
     */
    public SendSmsCodeVo sendSmsCode(SendSmsCodeParam param) {
        param.requireComplete();
        AppLoginContext context = param.getContext();
        // 保留 APK 传输时的字段顺序；签名使用下游发送的同一份 JSON 字节。
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("sendType", 3);
        body.put("mainBrand", "KFC");
        body.put("tdid", context.getTdid());
        body.putArray("encodeList").add("phone");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(KfcUpstream.APP_LOGIN, "/api/user/sendSmsCode", body,
                context.headers()), SendSmsCodeVo.class);
    }

    /**
     * 调用 APK 声明的 {@code GET https://applogin.kfcapp.cn/api/svc/startCaptcha}。
     * 证据：{@code UniteStartCaptchaApi.java}；本域名尚无 Reqable 成功响应。
     * @param rt APK 的验证类型 1 或 2
     * @param context 同次登录的客户端上下文
     * @return 验证注册数据
     */
    public StartCaptchaVo startCaptcha(int rt, AppLoginContext context) {
        if (rt != 1 && rt != 2) throw new IllegalArgumentException("不支持的验证类型");
        context.requireComplete();
        return response(gateway.get(KfcUpstream.APP_LOGIN, "/api/svc/startCaptcha",
                Map.of("rt", Integer.toString(rt), "type", "MOBILE", "ct", "native"),
                context.headers()), StartCaptchaVo.class);
    }

    /**
     * 调用 APK 声明的 {@code POST https://applogin.kfcapp.cn/api/svc/to/user/sendSmsCode}。
     * 证据：{@code UniteSendSmsCodeSvcApi.java}；具体报文和响应尚待 Reqable 验证。
     * 调用方须在同一短信单次触发锁内执行，并且只调用一次。
     * @param param 原发码请求参数
     * @param proof 同次挑战的验证字段
     * @return 上游发码响应
     */
    public SendSmsCodeVo sendSmsCodeVerified(SendSmsCodeParam param, CaptchaProof proof) {
        param.requireComplete();
        proof.requireComplete();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("sendType", 3);
        body.put("mainBrand", "KFC");
        appendProof(body, proof);
        body.put("tdid", param.getContext().getTdid());
        body.putArray("encodeList").add("phone");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(KfcUpstream.APP_LOGIN, "/api/svc/to/user/sendSmsCode", body,
                param.getContext().headers()), SendSmsCodeVo.class);
    }

    /**
     * 调用 APK 声明的 {@code POST https://applogin.kfcapp.cn/api/svc/to/user/loginBySmsCode}。
     * 证据：{@code UniteLoginBySmsCodeSvcApi.java}；具体报文和响应尚待 Reqable 验证。
     * 调用方须在同一登录单次触发锁内执行，并且只调用一次。
     * @param param 原验证码登录参数
     * @param proof 同次挑战的验证字段
     * @return 上游登录结果
     */
    public LoginBySmsCodeVo loginBySmsCodeVerified(LoginBySmsCodeParam param, CaptchaProof proof) {
        param.requireComplete();
        proof.requireComplete();
        AppLoginContext context = param.getContext();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("smsCode", crypto.encrypt(param.getSmsCode()));
        body.put("deviceId", context.getDeviceId());
        body.put("tdid", context.getTdid());
        body.put("jPushRegId", context.getJPushRegId());
        body.put("gbCityCode", context.getCityCode());
        body.put("mainBrand", "KFC");
        body.putNull("subBrands");
        appendProof(body, proof);
        body.putArray("encodeList").add("phone").add("smsCode");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(KfcUpstream.APP_LOGIN, "/api/svc/to/user/loginBySmsCode", body,
                context.headers()), LoginBySmsCodeVo.class);
    }

    private static void appendProof(ObjectNode body, CaptchaProof proof) {
        body.put("gtChallenge", proof.getGtChallenge());
        body.put("gtValidate", proof.getGtValidate());
        body.put("gtSeccode", proof.getGtSeccode());
        body.put("userid", proof.getUserid());
        body.put("gtServerStatus", proof.getGtServerStatus());
        body.put("rt", Integer.toString(proof.getRt()));
        body.put("event_id", proof.getEventId());
    }

    /**
     * 调用 {@code applogin.kfcapp.cn} 的 {@code POST /api/user/loginBySmsCode}。
     * APK/Reqable 证据：{@code 抓包文件/登录/02-[6374]-login-by-sms-code}；
     * 已确认 {@code errCode=0} 和 {@code data.mainBrandData}，可选跨品牌参数尚未覆盖。
     * 调用会改变上游登录会话，必须由上层执行当次确认和单次触发锁。
     *
     * @param param 用户本人提供的验证码、授权手机号和真实客户端上下文
     * @return 内部登录结果，所含上游 token 不应直接下发给 UniApp
     * @throws IllegalStateException 必需的本机配置缺失或响应无法解析时
     */
    public LoginBySmsCodeVo loginBySmsCode(LoginBySmsCodeParam param) {
        param.requireComplete();
        AppLoginContext context = param.getContext();
        ObjectNode body = baseBody(context);
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("smsCode", crypto.encrypt(param.getSmsCode()));
        body.put("deviceId", context.getDeviceId());
        body.put("jPushRegId", context.getJPushRegId());
        body.put("gbCityCode", context.getCityCode());
        body.putArray("encodeList").add("phone").add("smsCode");
        return response(gateway.post(KfcUpstream.APP_LOGIN, "/api/user/loginBySmsCode", body,
                context.headers()), LoginBySmsCodeVo.class);
    }

    private ObjectNode baseBody(AppLoginContext context) {
        ObjectNode body = mapper.createObjectNode();
        body.put("mainBrand", "KFC");
        body.put("tdid", context.getTdid());
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return body;
    }

    private <T> T response(JsonNode json, Class<T> type) {
        if (json == null || !json.isObject()) {
            throw new IllegalStateException("App login upstream returned an invalid JSON response");
        }
        try {
            return mapper.treeToValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("App login upstream response could not be parsed", e);
        }
    }
}
