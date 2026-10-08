package com.wanghui.kfc.kfcapi;

/** 已抓包确认的 App 登录上游与首批静态候选上游的标识。 */
public enum Upstream {
    /** 点餐与菜单。 */
    ORDERING,
    /** 已抓包确认的 React Native 选店与预点餐服务。 */
    RN_ORDER,
    /** 静态代码中的登录与用户身份候选服务。 */
    LOGIN,
    /** 已抓包确认的 App 短信登录服务。 */
    APP_LOGIN,
    /** 优惠券。 */
    COUPON,
    /** 商城与活动商品。 */
    MALL,
    /** 会员卡。 */
    PRIME
}
