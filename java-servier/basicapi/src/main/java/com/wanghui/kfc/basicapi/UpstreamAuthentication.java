package com.wanghui.kfc.basicapi;

import org.springframework.http.HttpHeaders;

/** 上游鉴权扩展点；每个已验证域名按自身协议实现。 */
public interface UpstreamAuthentication {
    /**
     * 判断当前实现是否负责指定上游。
     *
     * <p>默认返回 {@code true} 以保持测试和单一鉴权实现的函数式接口用法；
     * 生产环境中的域名专用实现必须覆盖此方法。</p>
     *
     * @param upstream 目标上游
     * @return 当前实现是否负责该上游
     */
    default boolean supports(Upstream upstream) {
        return true;
    }

    /**
     * 为一次上游请求添加所需的鉴权头，不应把真实凭据写入日志。
     *
     * @param upstream 目标上游
     * @param path 固定 API 路径
     * @param bodyJson 实际发送的 JSON 字符串；GET 请求传入按字段名排序的查询串
     * @param headers 待补充的请求头
     */
    void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers);
}
