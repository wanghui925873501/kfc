package com.wanghui.kfc.phhsapi.apploginphdappcn.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 按 PHHS 抓包协议计算 JSON POST 请求的小写 MD5 签名。 */
public final class PhhsRequestSignature {
    /** 阻止创建纯计算工具实例。 */
    private PhhsRequestSignature() { }

    /**
     * 对实际发送的 PHHS JSON 字符串计算 POST 签名。
     *
     * @param clientKey PHHS 客户端标识
     * @param clientSecret PHHS 签名密钥
     * @param timestamp 毫秒时间戳文本
     * @param path APK 使用的短签名路径
     * @param bodyJson 实际发送的请求体 JSON 字符串
     * @return 小写十六进制签名
     */
    public static String signPost(String clientKey, String clientSecret, String timestamp,
                                  String path, String bodyJson) {
        return md5(clientKey + "\t" + clientSecret + "\t" + timestamp
                + "\t" + path + "\t\t" + bodyJson);
    }

    private static String md5(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is unavailable", e);
        }
    }
}
