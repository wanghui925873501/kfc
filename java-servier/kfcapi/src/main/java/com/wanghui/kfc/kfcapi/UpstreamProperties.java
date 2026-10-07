package com.wanghui.kfc.kfcapi;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 上游启用状态与固定 HTTPS 域名配置。 */
@ConfigurationProperties(prefix = "kfc.upstream")
public class UpstreamProperties {
    /** 外部请求总开关；默认关闭，避免未联调时访问生产域名。 */
    private boolean enabled;
    /** 五个上游服务与其 HTTPS 基础地址的映射。 */
    private Map<Upstream, URI> urls = new EnumMap<>(Upstream.class);

    /** 供 Spring 绑定上游配置。 */
    public UpstreamProperties() { }

    /**
     * 读取上游请求开关。
     * @return 是否允许发起上游请求
     */
    public boolean isEnabled() { return enabled; }
    /**
     * 设置上游请求开关。
     * @param enabled 是否允许发起上游请求
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /**
     * 读取上游地址映射。
     * @return 上游服务到基础 URI 的映射
     */
    public Map<Upstream, URI> getUrls() { return urls; }
    /**
     * 设置上游地址映射。
     * @param urls 上游服务到基础 URI 的映射
     */
    public void setUrls(Map<Upstream, URI> urls) { this.urls = urls; }

    /**
     * 取得并校验一个上游的基础 URI，拒绝非 HTTPS、用户信息和路径片段。
     *
     * @param upstream 上游服务
     * @return 已校验的基础 URI
     * @throws IllegalStateException 配置缺失或 URI 不符合约束时
     */
    public URI url(Upstream upstream) {
        URI uri = urls.get(upstream);
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalStateException("Invalid HTTPS base URL for " + upstream);
        }
        return uri;
    }
}
