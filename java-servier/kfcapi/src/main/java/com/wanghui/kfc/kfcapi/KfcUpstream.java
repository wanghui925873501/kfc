package com.wanghui.kfc.kfcapi;

import com.wanghui.kfc.basicapi.Brand;
import com.wanghui.kfc.basicapi.Upstream;
import java.net.URI;

/** 定义仅由 KFC 模块使用的固定上游服务。 */
public enum KfcUpstream implements Upstream {
    /** 点餐与菜单静态候选服务。 */
    ORDERING("kfc-ordering", "https://order.kfc.com.cn", false),
    /** 已抓包确认的 React Native 选店与预点餐服务。 */
    RN_ORDER("kfc-rn-order", "https://rnorder.kfc.com.cn", false),
    /** 静态代码中的登录与用户身份候选服务。 */
    LOGIN("kfc-login", "https://login.kfc.com.cn", false),
    /** 已抓包确认的 KFC App 短信登录服务。 */
    APP_LOGIN("kfc-app-login", "https://applogin.kfcapp.cn", true),
    /** KFC 优惠券服务。 */
    COUPON("kfc-coupon", "https://appcoupon.kfc.com.cn", false),
    /** KFC 商城与活动商品服务。 */
    MALL("kfc-mall", "https://appmall.kfc.com.cn", false),
    /** KFC 会员卡服务。 */
    PRIME("kfc-prime", "https://appprime.kfc.com.cn", false);

    /** 通用配置中的唯一地址键。 */
    private final String configurationKey;
    /** 未覆盖配置时使用的固定生产基础地址。 */
    private final URI defaultUrl;
    /** 是否发送 KFC 登录协议要求的 gzip JSON 头。 */
    private final boolean gzipJson;

    KfcUpstream(String configurationKey, String defaultUrl, boolean gzipJson) {
        this.configurationKey = configurationKey;
        this.defaultUrl = URI.create(defaultUrl);
        this.gzipJson = gzipJson;
    }

    /** {@inheritDoc} */
    @Override
    public Brand brand() {
        return Brand.KFC;
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
