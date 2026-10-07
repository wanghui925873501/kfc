package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;

/** 上游鉴权扩展点；协议确认后使用正式服务端凭据实现。 */
public interface UpstreamAuthentication {
    /**
     * 为一次上游请求添加所需的鉴权头，不应把真实凭据写入日志。
     *
     * @param upstream 目标上游
     * @param path 固定 API 路径
     * @param body 请求 JSON；GET 请求传入 JSON null 节点
     * @param headers 待补充的请求头
     */
    void apply(Upstream upstream, String path, JsonNode body, HttpHeaders headers);
}
