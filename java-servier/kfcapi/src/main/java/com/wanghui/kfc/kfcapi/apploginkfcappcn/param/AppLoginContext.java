package com.wanghui.kfc.kfcapi.apploginkfcappcn.param;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 一次授权测试请求所需的设备信息和客户端头，由调用方逐项设置真实上下文。 */
@Data
@AllArgsConstructor
public class AppLoginContext {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public AppLoginContext() { }

    /** 请求体中的设备追踪标识。 */
    @ToString.Exclude
    private String tdid;
    /** 请求体及客户端头中的设备标识。 */
    @ToString.Exclude
    private String deviceId;
    /** 登录请求中的推送注册标识，可以是空字符串。 */
    @ToString.Exclude
    private String jPushRegId;
    /** 客户端城市编码。 */
    private String cityCode;
    /** 客户端渠道。 */
    private String channel;
    /** 当前客户端用户编码，未登录时可以是空字符串。 */
    @ToString.Exclude
    private String userCode;
    /** 客户端每次请求提供的风控请求头。 */
    @ToString.Exclude
    private String rcsdcid;
    /** 客户端版本请求头。 */
    private String rcsav;
    /** 客户端标识请求头。 */
    private String userAgent;

    /** 校验必需的请求上下文，避免悄悄伪造缺失的客户端字段。 */
    public void requireComplete() {
        Objects.requireNonNull(tdid, "tdid");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(jPushRegId, "jPushRegId");
        Objects.requireNonNull(cityCode, "cityCode");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(userCode, "userCode");
        Objects.requireNonNull(rcsdcid, "rcsdcid");
        Objects.requireNonNull(rcsav, "rcsav");
        Objects.requireNonNull(userAgent, "userAgent");
    }

    /**
     * 构造抓包中出现的客户端业务请求头。
     * @return 不含签名和密钥的客户端请求头
     */
    public Map<String, String> headers() {
        requireComplete();
        return Map.of("x-yumc-client-channel", channel,
                "x-yumc-client-deviceid", deviceId,
                "x-yumc-client-citycode", cityCode,
                "x-yumc-client-usercode", userCode,
                "rcsdcid", rcsdcid, "rcsav", rcsav, "user-agent", userAgent);
    }
}
