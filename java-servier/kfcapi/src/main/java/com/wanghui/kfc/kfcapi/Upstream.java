package com.wanghui.kfc.kfcapi;

/** 五个首批建模的肯德基上游服务，实际 API 域名仍需联调核实。 */
public enum Upstream {
    /** 点餐与菜单。 */
    ORDERING,
    /** 登录与用户身份。 */
    LOGIN,
    /** 优惠券。 */
    COUPON,
    /** 商城与活动商品。 */
    MALL,
    /** 会员卡。 */
    PRIME
}
