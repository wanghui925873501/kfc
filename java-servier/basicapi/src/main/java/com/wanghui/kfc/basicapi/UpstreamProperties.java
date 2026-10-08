package com.wanghui.kfc.basicapi;

import java.net.URI;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 保存按品牌隔离的上游开关和可选 HTTPS 地址覆盖。 */
@Data
@ConfigurationProperties(prefix = "basic.upstream")
public class UpstreamProperties {
    /** 各品牌外部请求开关；未配置的品牌默认关闭。 */
    private Map<Brand, Boolean> enabled = new EnumMap<>(Brand.class);
    /** 以品牌模块提供的唯一配置键索引的基础地址覆盖。 */
    private Map<String, URI> urls = new HashMap<>();

    /** 供 Spring 绑定通用上游配置。 */
    public UpstreamProperties() { }

    /**
     * 判断指定服务所属品牌是否允许访问生产上游。
     * @param upstream 品牌模块定义的上游服务
     * @return 仅显式启用时返回 true
     */
    public boolean isEnabled(Upstream upstream) {
        return Boolean.TRUE.equals(enabled.get(upstream.brand()));
    }

    /**
     * 取得并校验一个上游的基础 URI，拒绝非 HTTPS、用户信息和路径片段。
     *
     * @param upstream 上游服务
     * @return 已校验的基础 URI
     * @throws IllegalStateException 配置缺失或 URI 不符合约束时
     */
    public URI url(Upstream upstream) {
        URI uri = urls.getOrDefault(upstream.configurationKey(), upstream.defaultUrl());
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalStateException("Invalid HTTPS base URL for " + upstream);
        }
        return uri;
    }
}
