package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 按已抓包验证的 DES/CBC/PKCS5Padding 规则加密登录敏感字段。 */
@Component
public class AppLoginCrypto {
    /** 本机加密配置。 */
    private final AppLoginProperties properties;

    /**
     * 创建登录字段加密器。
     * @param properties 本机加密配置
     */
    public AppLoginCrypto(AppLoginProperties properties) { this.properties = properties; }

    /**
     * 将手机号或验证码加密为上游要求的 Base64 文本。
     *
     * @param plainText 待加密的敏感字段
     * @return DES 加密后的 Base64 文本
     * @throws IllegalStateException 密钥未配置、长度错误或本机加密失败时
     */
    public String encrypt(String plainText) {
        byte[] key = properties.requireDesKey().getBytes(StandardCharsets.UTF_8);
        if (key.length != 8) {
            throw new IllegalStateException("KFC_DES_KEY must contain exactly eight UTF-8 bytes");
        }
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"), new IvParameterSpec(key));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("App login field encryption failed", e);
        }
    }
}
