package com.wanghui.kfc.server.identity;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 使用独立的 AES-GCM 密钥加密数据库中的 token 和 Redis 中的会话引用。 */
@Component
public class KfcSessionCipher {
    /** 会话密钥配置。 */
    private final KfcSessionProperties properties;
    /** 为每次加密生成独立随机数的安全随机源。 */
    private final SecureRandom random = new SecureRandom();

    /**
     * 创建会话加密器。
     * @param properties 会话密钥配置
     */
    public KfcSessionCipher(KfcSessionProperties properties) { this.properties = properties; }

    /**
     * 加密一次敏感内容，并将版本、随机数与密文共同编码。
     * @param plainText token 或会话引用的 UTF-8 字节
     * @return 可保存到数据库或 Redis 的密文文本
     * @throws IllegalStateException 密钥未配置或加密失败时
     */
    public String encrypt(byte[] plainText) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plainText);
            ByteBuffer payload = ByteBuffer.allocate(1 + nonce.length + encrypted.length);
            payload.put((byte) 1).put(nonce).put(encrypted);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("KFC session encryption failed", e);
        }
    }

    /**
     * 验证完整性并解密数据库 token 或 Redis 会话内容。
     * @param encoded 带版本和随机数的密文文本
     * @return 原始 UTF-8 字节
     * @throws IllegalStateException 密钥缺失、密文损坏或版本不支持时
     */
    public byte[] decrypt(String encoded) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encoded);
            if (payload.length < 29 || payload[0] != 1) {
                throw new IllegalStateException("Invalid KFC session format");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.get();
            byte[] nonce = new byte[12];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            return cipher.doFinal(encrypted);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("Invalid KFC session ciphertext", e);
        }
    }

    private SecretKeySpec key() {
        String encoded = properties.getKeyBase64();
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("KFC_SESSION_KEY_BASE64 is not configured");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("KFC_SESSION_KEY_BASE64 is invalid", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("KFC_SESSION_KEY_BASE64 must encode 32 bytes");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
