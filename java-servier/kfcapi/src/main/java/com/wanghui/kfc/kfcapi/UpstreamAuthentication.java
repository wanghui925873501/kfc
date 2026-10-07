package com.wanghui.kfc.kfcapi;

import org.springframework.http.HttpHeaders;

/** 上游鉴权扩展点；每个已验证域名按自身协议实现。 */
public interface UpstreamAuthentication {
    /**
     * 为一次上游请求添加所需的鉴权头，不应把真实凭据写入日志。
     *
     * @param upstream 目标上游
     * @param path 固定 API 路径
     * @param bodyJson 实际发送的 JSON 字符串；GET 请求传入空字符串
     * @param headers 待补充的请求头
     */
    void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers);
}
