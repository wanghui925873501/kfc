package com.wanghui.kfc.kfcapi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 计算 KFC 已验证 GET/POST 协议共用的小写 MD5 请求签名。 */
public final class KfcRequestSignature {
    /** 阻止创建纯计算工具实例。 */
    private KfcRequestSignature() { }

    /**
     * 对实际发送的 JSON 字符串计算 POST 签名。
     *
     * @param clientKey 客户端标识
     * @param clientSecret 服务端保存的签名密钥
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

    /**
     * 对按字段名排序的查询串计算 GET 签名。
     *
     * @param clientKey 客户端标识
     * @param clientSecret 服务端保存的签名密钥
     * @param timestamp 毫秒时间戳文本
     * @param path APK 使用的短签名路径
     * @param query 按字段名排序、未进行 URL 编码的查询串
     * @return 小写十六进制签名
     */
    public static String signGet(String clientKey, String clientSecret, String timestamp,
                                 String path, String query) {
        return md5(clientKey + "\t" + clientSecret + "\t" + timestamp
                + "\t" + path + "\t" + query);
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
