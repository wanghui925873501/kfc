package com.wanghui.kfc.phhsapi.apploginphdappcn.support;


import com.wanghui.kfc.basicapi.Upstream;
import com.wanghui.kfc.basicapi.UpstreamAuthentication;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 为必胜客统一登录及风控验证接口添加 APK 对应的 KBS 短路径签名。 */
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
     * 为短信登录和极验风控请求添加动态签名头。
     * @param upstream 固定 PHHS 登录上游
     * @param path 传输路径
     * @param bodyJson POST 的 JSON 字符串或 GET 的排序查询串
     * @param headers 待补充的请求头
     */
    @Override
    public void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers) {
        if (upstream != PhhsUpstream.APP_LOGIN) {
            throw new IllegalStateException("PHHS login authentication is not configured for this request");
        }
        String signPath = switch (path) {
            case "/api/user/sendSmsCode" -> "/user/sendSmsCode";
            case "/api/user/loginBySmsCode" -> "/user/loginBySmsCode";
            case "/api/svc/startCaptcha" -> "/svc/startCaptcha";
            case "/api/svc/to/user/sendSmsCode" -> "/svc/to/user/sendSmsCode";
            case "/api/svc/to/user/loginBySmsCode" -> "/svc/to/user/loginBySmsCode";
            default -> throw new IllegalStateException(
                    "PHHS login authentication is not configured for this path");
        };
        if (bodyJson.isEmpty() && !"/svc/startCaptcha".equals(signPath)) {
            throw new IllegalStateException("PHHS login authentication is not configured for this request");
        }
        String key = properties.requireClientKey();
        String secret = properties.requireClientSecret();
        String timestamp = Long.toString(clock.millis());
        headers.set("kbck", key);
        headers.set("kbcts", timestamp);
        headers.set("kbsv", "/svc/startCaptcha".equals(signPath)
                ? PhhsRequestSignature.signGet(key, secret, timestamp, signPath, bodyJson)
                : PhhsRequestSignature.signPost(key, secret, timestamp, signPath, bodyJson));
    }
}
