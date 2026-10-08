package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import com.wanghui.kfc.kfcapi.Upstream;
import com.wanghui.kfc.kfcapi.UpstreamAuthentication;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 为 App 登录上游的已知路径计算 GET/POST 签名，其他域名继续拒绝请求。 */
@Component
public class AppLoginAuthentication implements UpstreamAuthentication {
    /** 本机签名配置。 */
    private final AppLoginProperties properties;
    /** 签名使用的当前时间来源。 */
    private final Clock clock;

    /**
     * 创建生产环境签名器。
     * @param properties 本机签名配置
     */
    @Autowired
    public AppLoginAuthentication(AppLoginProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AppLoginAuthentication(AppLoginProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 仅处理 {@code applogin.kfcapp.cn} 上游。
     *
     * @param upstream 目标上游
     * @return 目标是否为 App 登录服务
     */
    @Override
    public boolean supports(Upstream upstream) {
        return upstream == Upstream.APP_LOGIN;
    }

    /**
     * 为已验证的 App 登录接口添加签名头。
     *
     * @param upstream 固定上游服务
     * @param path 实际请求的固定传输路径，签名时映射为 APK 的 urlMethod 短路径
     * @param bodyJson 实际发送的请求 JSON
     * @param headers 待补充的请求头
     * @throws IllegalStateException 其他域名或凭据未配置时
     */
    @Override
    public void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers) {
        if (upstream != Upstream.APP_LOGIN) {
            throw new IllegalStateException("Upstream authentication is not configured for " + upstream);
        }
        String signPath = switch (path) {
            case "/api/user/sendSmsCode" -> "/user/sendSmsCode";
            case "/api/user/loginBySmsCode" -> "/user/loginBySmsCode";
            case "/api/svc/startCaptcha" -> "/svc/startCaptcha";
            case "/api/svc/to/user/sendSmsCode" -> "/svc/to/user/sendSmsCode";
            case "/api/svc/to/user/loginBySmsCode" -> "/svc/to/user/loginBySmsCode";
            default -> throw new IllegalStateException("App login authentication is not configured for this request");
        };
        if (bodyJson.isEmpty() && !"/svc/startCaptcha".equals(signPath)) {
            throw new IllegalStateException("App login authentication is not configured for this request");
        }
        String key = properties.requireClientKey();
        String secret = properties.requireClientSecret();
        String timestamp = Long.toString(clock.millis());
        headers.set("kbck", key);
        headers.set("kbcts", timestamp);
        headers.set("kbsv", "/svc/startCaptcha".equals(signPath)
                ? AppLoginSignature.signGet(key, secret, timestamp, signPath, bodyJson)
                : AppLoginSignature.sign(key, secret, timestamp, signPath, bodyJson));
    }
}
