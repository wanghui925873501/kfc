package com.wanghui.kfc.phhsapi.apploginphdappcn.support;


import com.wanghui.kfc.basicapi.Upstream;
import com.wanghui.kfc.basicapi.UpstreamAuthentication;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 为必胜客统一登录接口添加 APK 已验证的 KBS 短路径签名。 */
@Component
public class PhhsLoginAuthentication implements UpstreamAuthentication {
    /** 本机 PHHS 签名配置。 */
    private final PhhsLoginProperties properties;
    /** 请求时间戳来源。 */
    private final Clock clock;

    /**
     * 创建生产环境 PHHS 签名器。
     * @param properties 本机 PHHS 配置
     */
    @Autowired
    public PhhsLoginAuthentication(PhhsLoginProperties properties) {
        this(properties, Clock.systemUTC());
    }

    PhhsLoginAuthentication(PhhsLoginProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 判断是否负责必胜客统一登录上游。
     * @param upstream 目标上游
     * @return 仅 PHHS 登录上游返回 true
     */
    @Override
    public boolean supports(Upstream upstream) {
        return upstream == PhhsUpstream.APP_LOGIN;
    }

    /**
     * 为短信发送或验证码登录请求添加动态签名头。
     * @param upstream 固定 PHHS 登录上游
     * @param path 传输路径
     * @param bodyJson 实际发送的 JSON 字符串
     * @param headers 待补充的请求头
     */
    @Override
    public void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers) {
        if (upstream != PhhsUpstream.APP_LOGIN || bodyJson.isEmpty()) {
            throw new IllegalStateException("PHHS login authentication is not configured for this request");
        }
        String signPath = switch (path) {
            case "/api/user/sendSmsCode" -> "/user/sendSmsCode";
            case "/api/user/loginBySmsCode" -> "/user/loginBySmsCode";
            default -> throw new IllegalStateException(
                    "PHHS login authentication is not configured for this path");
        };
        String key = properties.requireClientKey();
        String secret = properties.requireClientSecret();
        String timestamp = Long.toString(clock.millis());
        headers.set("kbck", key);
        headers.set("kbcts", timestamp);
        headers.set("kbsv", PhhsRequestSignature.signPost(
                key, secret, timestamp, signPath, bodyJson));
    }
}
