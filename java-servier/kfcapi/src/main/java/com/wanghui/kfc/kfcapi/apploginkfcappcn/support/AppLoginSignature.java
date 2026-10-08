package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import com.wanghui.kfc.kfcapi.KfcRequestSignature;

/** 依据 APK 中的 GET/POST 规则计算 App 登录上游签名。 */
final class AppLoginSignature {
    /** 阻止创建纯计算工具实例。 */
    private AppLoginSignature() { }

    /**
     * 对实际发送的 JSON 字符串计算小写 MD5 签名。
     *
     * @param clientKey 客户端标识
     * @param clientSecret 服务端保存的签名密钥
     * @param timestamp 毫秒时间戳文本
     * @param path APK 的 urlMethod 短路径，不包含传输 URL 的 /api 前缀
     * @param bodyJson 实际发送的请求体 JSON 字符串
     * @return 小写十六进制签名
     */
    static String sign(String clientKey, String clientSecret, String timestamp, String path,
                       String bodyJson) {
        return KfcRequestSignature.signPost(clientKey, clientSecret, timestamp, path, bodyJson);
    }

    /**
     * 按 APK 的 GET 签名格式处理按字段名排序的查询串。
     * @param clientKey 客户端标识
     * @param clientSecret 本机签名密钥
     * @param timestamp 毫秒时间戳
     * @param path APK 中的短路径
     * @param query 按字段名排序、未进行 URL 编码的查询串
     * @return 小写十六进制签名
     */
    static String signGet(String clientKey, String clientSecret, String timestamp, String path,
                          String query) {
        return KfcRequestSignature.signGet(clientKey, clientSecret, timestamp, path, query);
    }
}
