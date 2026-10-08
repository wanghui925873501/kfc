package com.wanghui.kfc.phhsapi.apploginphdappcn.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsCaptchaProof;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginCrypto;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginProperties;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsSendSmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsStartCaptchaVo;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 按真实 Reqable 证据封装 {@code applogin.phdapp.cn} 的 PHHS 短信登录接口。 */
@Component
public class PhhsLoginApi {
    /** 受固定 HTTPS 域名和路径限制的传输层。 */
    private final UpstreamGateway gateway;
    /** PHHS 手机号和验证码加密器。 */
    private final PhhsLoginCrypto crypto;
    /** PHHS 请求体固定配置。 */
    private final PhhsLoginProperties properties;
    /** 请求构造及响应解析器。 */
    private final ObjectMapper mapper;

    /**
     * 创建必胜客统一登录客户端。
     * @param gateway 固定上游传输层
     * @param crypto PHHS 敏感字段加密器
     * @param properties PHHS 本机配置
     * @param mapper JSON 工具
     */
    public PhhsLoginApi(UpstreamGateway gateway, PhhsLoginCrypto crypto,
                        PhhsLoginProperties properties, ObjectMapper mapper) {
        this.gateway = gateway;
        this.crypto = crypto;
        this.properties = properties;
        this.mapper = mapper;
    }

    /**
     * 调用 {@code POST https://applogin.phdapp.cn/api/user/sendSmsCode}。
     * Reqable 证据：{@code D:/wanghui/bskuniapp/抓包文件/登录/01-[371]-send-sms-code}。
     * 会真实发送短信，调用方必须先取得当次授权并建立原子单次触发锁。
     *
     * @param param 授权手机号和真实 PHHS 安装上下文
     * @return 上游短信响应；会话 371 未保存响应体，成功结构仍待 Java 实测
     */
    public PhhsSendSmsCodeVo sendSmsCode(PhhsSendSmsCodeParam param) {
        param.requireComplete();
        PhhsLoginContext context = param.getContext();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("sendType", 3);
        body.put("mainBrand", "PHHS");
        body.put("tdid", context.getTdid());
        body.putArray("encodeList").add("phone");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(PhhsUpstream.APP_LOGIN, "/api/user/sendSmsCode",
                body, context.headers()), PhhsSendSmsCodeVo.class);
    }

    /**
     * 调用 PHHS APK 声明的 {@code GET /api/svc/startCaptcha} 注册一次极验挑战。
     * APK 证据：{@code UniteStartCaptchaApi.java}；本域名真实响应仍待抓包确认。
     *
     * @param rt APK 的验证类型 1 或 2
     * @param context 同次登录使用的 PHHS 客户端上下文
     * @return PHHS 极验注册数据
     */
    public PhhsStartCaptchaVo startCaptcha(int rt, PhhsLoginContext context) {
        if (rt != 1 && rt != 2) {
            throw new IllegalArgumentException("不支持的 PHHS 验证类型");
        }
        context.requireComplete();
        return response(gateway.get(PhhsUpstream.APP_LOGIN, "/api/svc/startCaptcha",
                Map.of("rt", Integer.toString(rt), "type", "MOBILE", "ct", "native"),
                context.headers()), PhhsStartCaptchaVo.class);
    }

    /**
     * 调用 PHHS APK 声明的 {@code POST /api/svc/to/user/sendSmsCode}。
     * APK 证据：{@code UniteSendSmsCodeSvcApi.java}；真实报文仍待 Reqable 验证。
     * 调用方必须在原短信单次锁内执行，并且最多补发一次。
     *
     * @param param 原 PHHS 发码参数
     * @param proof 同次风险挑战的小辉版验证结果
     * @return PHHS 最终发码响应
     */
    public PhhsSendSmsCodeVo sendSmsCodeVerified(
            PhhsSendSmsCodeParam param, PhhsCaptchaProof proof) {
        param.requireComplete();
        proof.requireComplete();
        PhhsLoginContext context = param.getContext();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("sendType", 3);
        body.put("mainBrand", "PHHS");
        appendProof(body, proof);
        body.put("tdid", context.getTdid());
        body.putArray("encodeList").add("phone");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(PhhsUpstream.APP_LOGIN,
                "/api/svc/to/user/sendSmsCode", body, context.headers()),
                PhhsSendSmsCodeVo.class);
    }

    /**
     * 调用 {@code POST https://applogin.phdapp.cn/api/user/loginBySmsCode}。
     * Reqable 证据：{@code D:/wanghui/bskuniapp/抓包文件/登录/02-[497]-login-by-sms-code}，
     * 已验证 HTTP 200、业务 {@code errCode=0} 和主品牌登录数据。
     * 调用会改变上游登录会话，调用方必须取得当次授权并建立独立原子锁。
     *
     * @param param 同一手机号、六位验证码和真实 PHHS 安装上下文
     * @return 包含后端专用 token 与会员标识的登录结果
     */
    public PhhsLoginBySmsCodeVo loginBySmsCode(PhhsLoginBySmsCodeParam param) {
        param.requireComplete();
        PhhsLoginContext context = param.getContext();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("smsCode", crypto.encrypt(param.getSmsCode()));
        body.put("deviceId", context.getDeviceId());
        body.put("tdid", context.getTdid());
        body.put("jPushRegId", context.getJPushRegId());
        body.put("gbCityCode", context.getCityCode());
        body.put("mainBrand", "PHHS");
        body.putArray("encodeList").add("phone").add("smsCode");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(PhhsUpstream.APP_LOGIN, "/api/user/loginBySmsCode",
                body, context.headers()), PhhsLoginBySmsCodeVo.class);
    }

    /**
     * 调用 PHHS APK 声明的 {@code POST /api/svc/to/user/loginBySmsCode}。
     * APK 证据：{@code UniteLoginBySmsCodeSvcApi.java}；真实报文仍待 Reqable 验证。
     * 调用方必须在原登录单次锁内执行，并且最多补登一次。
     *
     * @param param 原 PHHS 验证码登录参数
     * @param proof 同次风险挑战的小辉版验证结果
     * @return PHHS 最终登录响应
     */
    public PhhsLoginBySmsCodeVo loginBySmsCodeVerified(
            PhhsLoginBySmsCodeParam param, PhhsCaptchaProof proof) {
        param.requireComplete();
        proof.requireComplete();
        PhhsLoginContext context = param.getContext();
        ObjectNode body = mapper.createObjectNode();
        body.put("phone", crypto.encrypt(param.getPhone()));
        body.put("smsCode", crypto.encrypt(param.getSmsCode()));
        body.put("deviceId", context.getDeviceId());
        body.put("tdid", context.getTdid());
        body.put("jPushRegId", context.getJPushRegId());
        body.put("gbCityCode", context.getCityCode());
        body.put("mainBrand", "PHHS");
        body.putNull("subBrands");
        appendProof(body, proof);
        body.putArray("encodeList").add("phone").add("smsCode");
        body.put("isFromCustomerClient", true);
        body.put("secretKey", properties.requireRequestSecretKey());
        return response(gateway.post(PhhsUpstream.APP_LOGIN,
                "/api/svc/to/user/loginBySmsCode", body, context.headers()),
                PhhsLoginBySmsCodeVo.class);
    }

    private static void appendProof(ObjectNode body, PhhsCaptchaProof proof) {
        body.put("gtChallenge", proof.getGtChallenge());
        body.put("gtValidate", proof.getGtValidate());
        body.put("gtSeccode", proof.getGtSeccode());
        body.put("userid", proof.getUserid());
        body.put("gtServerStatus", proof.getGtServerStatus());
        body.put("rt", Integer.toString(proof.getRt()));
        body.put("event_id", proof.getEventId());
    }

    private <T> T response(JsonNode json, Class<T> type) {
        if (json == null || !json.isObject()) {
            throw new IllegalStateException("PHHS login upstream returned an invalid JSON response");
        }
        try {
            return mapper.treeToValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PHHS login upstream response could not be parsed", e);
        }
    }
}
