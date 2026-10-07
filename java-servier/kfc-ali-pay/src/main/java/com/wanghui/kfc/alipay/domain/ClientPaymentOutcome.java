package com.wanghui.kfc.alipay.domain;

/**
 * UniApp 或浏览器上报的非权威支付结果。
 */
public enum ClientPaymentOutcome {
    /** 客户端显示支付成功。 */
    SUCCESS,
    /** 客户端显示支付失败。 */
    FAILURE,
    /** 用户取消支付。 */
    CANCELLED,
    /** 页面关闭、断连或结果不确定。 */
    UNKNOWN
}
