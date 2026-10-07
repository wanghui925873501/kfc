package com.wanghui.kfc.kfcapi;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 上游启用状态与固定 HTTPS 域名配置。 */
@Data
@ConfigurationProperties(prefix = "kfc.upstream")
public class UpstreamProperties {
    /** 供 Spring 绑定上游地址配置。 */
    public UpstreamProperties() { }

    /** 外部请求总开关；默认关闭，避免未联调时访问生产域名。 */
    private boolean enabled;
    /** 已确认服务及静态候选服务与 HTTPS 基础地址的映射。 */
    private Map<Upstream, URI> urls = new EnumMap<>(Upstream.class);

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
