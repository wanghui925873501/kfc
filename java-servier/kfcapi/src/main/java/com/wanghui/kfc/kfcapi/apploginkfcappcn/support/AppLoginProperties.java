package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本机环境提供的 App 登录请求参数与签名材料。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.app-login")
public class AppLoginProperties {
    /** 供 Spring 绑定本机登录配置。 */
    public AppLoginProperties() { }

    /** 签名请求头 {@code kbck} 对应的客户端标识。 */
    @ToString.Exclude
    private String clientKey;
    /** 服务端计算签名使用的客户端密钥。 */
    @ToString.Exclude
    private String clientSecret;
    /** 敏感字段 DES 加密所需的八字节密钥。 */
    @ToString.Exclude
    private String desKey;
    /** 请求体 {@code secretKey} 字段的本机配置值。 */
    @ToString.Exclude
    private String requestSecretKey;

    /**
     * 在发送请求前确认客户端标识已配置。
     * @return 已配置的客户端标识
     */
    public String requireClientKey() { return required(clientKey, "KFC_CLIENT_KEY"); }

    /**
     * 在发送请求前确认签名密钥已配置。
     * @return 已配置的签名密钥
     */
    public String requireClientSecret() { return required(clientSecret, "KFC_CLIENT_SEC"); }

    /**
     * 在发送请求前确认 DES 密钥已配置。
     * @return 已配置的 DES 密钥
     */
    public String requireDesKey() { return required(desKey, "KFC_DES_KEY"); }

    /**
     * 在发送请求前确认请求体配置值已配置。
     * @return 已配置的请求体配置值
     */
    public String requireRequestSecretKey() {
        return required(requestSecretKey, "KFC_LOGIN_REQUEST_SECRET_KEY");
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not configured");
        }
        return value;
    }
}
