package com.wanghui.kfc.phhsapi;

import com.wanghui.kfc.basicapi.Brand;
import com.wanghui.kfc.basicapi.Upstream;
import java.net.URI;

/** 定义仅由 PHHS 模块使用的固定上游服务。 */
public enum PhhsUpstream implements Upstream {
    /** 已抓包确认的必胜客 App 短信登录服务。 */
    APP_LOGIN("phhs-app-login", "https://applogin.phdapp.cn", true);

    /** 通用配置中的唯一地址键。 */
    private final String configurationKey;
    /** 未覆盖配置时使用的固定生产基础地址。 */
    private final URI defaultUrl;
    /** 是否发送 PHHS 登录协议要求的 gzip JSON 头。 */
    private final boolean gzipJson;

    PhhsUpstream(String configurationKey, String defaultUrl, boolean gzipJson) {
        this.configurationKey = configurationKey;
        this.defaultUrl = URI.create(defaultUrl);
        this.gzipJson = gzipJson;
    }

    /** {@inheritDoc} */
    @Override
    public Brand brand() {
        return Brand.PHHS;
    }

    /** {@inheritDoc} */
    @Override
    public String configurationKey() {
        return configurationKey;
    }

    /** {@inheritDoc} */
    @Override
    public URI defaultUrl() {
        return defaultUrl;
    }

    /** {@inheritDoc} */
    @Override
    public boolean usesGzipJson() {
        return gzipJson;
    }
}
