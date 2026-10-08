package com.wanghui.kfc.basicapi;

import java.net.URI;

/** 由各品牌模块实现的上游服务定义，基础模块不登记具体品牌接口。 */
public interface Upstream {
    /**
     * 取得服务所属品牌。
     * @return 品牌标识
     */
    Brand brand();

    /**
     * 取得配置文件中的唯一地址键。
     * @return 跨品牌唯一的配置键
     */
    String configurationKey();

    /**
     * 取得 APK 或真实抓包确认的默认基础地址。
     * @return 不带业务路径的 HTTPS 地址
     */
    URI defaultUrl();

    /**
     * 判断请求是否需要明确发送 gzip 接收头和 UTF-8 JSON 类型。
     * @return 需要登录协议兼容头时为 true
     */
    default boolean usesGzipJson() {
        return false;
    }
}
