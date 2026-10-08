package com.wanghui.kfc.phhsapi.apploginphdappcn.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 按必胜客 APK 已验证的 DES/CBC/PKCS5Padding 规则加密登录敏感字段。 */
@Component
public class PhhsLoginCrypto {
    /** 本机 PHHS 加密配置。 */
    private final PhhsLoginProperties properties;

    /**
     * 创建登录字段加密器。
     * @param properties 本机 PHHS 配置
     */
    public PhhsLoginCrypto(PhhsLoginProperties properties) {
        this.properties = properties;
    }

    /**
     * 加密手机号或短信验证码。
     * @param plainText 待加密文本
     * @return Base64 编码的 DES 密文
     * @throws IllegalStateException 密钥无效或加密失败时
     */
    public String encrypt(String plainText) {
        byte[] key = properties.requireDesKey().getBytes(StandardCharsets.UTF_8);
        if (key.length != 8) {
            throw new IllegalStateException("PHHS_DES_KEY must contain exactly eight UTF-8 bytes");
        }
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"), new IvParameterSpec(key));
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PHHS login field encryption failed", e);
        }
    }
}
