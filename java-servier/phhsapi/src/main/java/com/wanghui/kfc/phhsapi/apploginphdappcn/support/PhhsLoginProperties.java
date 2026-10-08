package com.wanghui.kfc.phhsapi.apploginphdappcn.support;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 保存必胜客 App 登录协议所需的本机签名和加密配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "phhs.login")
public class PhhsLoginProperties {
    /** 供 Spring 绑定配置。 */
    public PhhsLoginProperties() { }

    /** 签名请求头 {@code kbck} 对应的客户端标识。 */
    @ToString.Exclude
    private String clientKey;
    /** 服务端校验 KBS 签名使用的客户端密钥。 */
    @ToString.Exclude
    private String clientSecret;
    /** 手机号和验证码 DES 加密所需的八字节密钥。 */
    @ToString.Exclude
    private String desKey;
    /** 请求体 {@code secretKey} 字段，正式 PHHS 客户端固定为 {@code phhs}。 */
    @ToString.Exclude
    private String requestSecretKey = "phhs";

    /**
     * 取得已配置的客户端标识。
     * @return 客户端标识
     */
    public String requireClientKey() {
        return required(clientKey, "PHHS_CLIENT_KEY");
    }

    /**
     * 取得已配置的签名密钥。
     * @return 签名密钥
     */
    public String requireClientSecret() {
        return required(clientSecret, "PHHS_CLIENT_SEC");
    }

    /**
     * 取得已配置的 DES 密钥。
     * @return DES 密钥
     */
    public String requireDesKey() {
        return required(desKey, "PHHS_DES_KEY");
    }

    /**
     * 取得已配置的请求体品牌键。
     * @return 请求体品牌键
     */
    public String requireRequestSecretKey() {
        return required(requestSecretKey, "PHHS_LOGIN_REQUEST_SECRET_KEY");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not configured");
        }
        return value;
    }
}
