package com.wanghui.kfc.kfcapi.rnorderkfccomcn.param;

import lombok.Data;
import lombok.ToString;

/** 保存一次已登录用户调用 RN 点餐上游所需的身份和客户端字段。 */
@Data
public class RnOrderContext {
    /** 供调用方逐项设置上下文。 */
    public RnOrderContext() { }

    /** 当前手机号绑定安装的设备标识。 */
    @ToString.Exclude
    private String deviceId;
    /** 登录响应返回的 KFC 用户编码。 */
    @ToString.Exclude
    private String userCode;
    /** 登录响应返回的上游票据，写入请求体 {@code ticket}。 */
    @ToString.Exclude
    private String ticket;
    /** 当前客户端档案使用的城市编码。 */
    private String cityCode;
    /** 当前客户端档案使用的 User-Agent。 */
    private String userAgent;

    /**
     * 校验已登录点餐上下文。
     *
     * @return 当前完整上下文
     * @throws IllegalArgumentException 必需字段缺失时
     */
    public RnOrderContext requireComplete() {
        if (isBlank(deviceId) || isBlank(userCode) || isBlank(ticket)
                || isBlank(cityCode) || isBlank(userAgent)) {
            throw new IllegalArgumentException("RN order context is incomplete");
        }
        return this;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
