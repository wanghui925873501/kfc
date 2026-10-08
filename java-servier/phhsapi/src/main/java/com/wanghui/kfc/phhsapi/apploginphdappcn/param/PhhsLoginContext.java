package com.wanghui.kfc.phhsapi.apploginphdappcn.param;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 描述一次 PHHS 登录请求所需的真实安装、城市和风险请求头。 */
@Data
@AllArgsConstructor
public class PhhsLoginContext {
    /** 供配置框架或调用方逐项设置字段。 */
    public PhhsLoginContext() { }

    /** 请求体中的设备追踪标识。 */
    @ToString.Exclude
    private String tdid;
    /** 请求体和业务请求头中的安装设备标识。 */
    @ToString.Exclude
    private String deviceId;
    /** 推送注册标识，未取得时允许为空字符串。 */
    @ToString.Exclude
    private String jPushRegId;
    /** 国标城市编码。 */
    private String cityCode;
    /** 客户端渠道，官方 App 使用 {@code app}。 */
    private String channel;
    /** 登录前允许为空字符串的客户端用户编码。 */
    @ToString.Exclude
    private String userCode;
    /** 风控 SDK 为当前安装提供的请求标识。 */
    @ToString.Exclude
    private String rcsdcid;
    /** 必胜客客户端版本号。 */
    private String rcsav;
    /** 官方客户端 User-Agent。 */
    @ToString.Exclude
    private String userAgent;

    /** 校验所有必需字段，且不为缺失字段生成伪造值。 */
    public void requireComplete() {
        required(tdid, "tdid");
        required(deviceId, "deviceId");
        nonNull(jPushRegId, "jPushRegId");
        required(cityCode, "cityCode");
        required(channel, "channel");
        nonNull(userCode, "userCode");
        required(rcsdcid, "rcsdcid");
        required(rcsav, "rcsav");
        required(userAgent, "userAgent");
    }

    /**
     * 构造抓包确认的客户端业务请求头。
     * @return 保持证据顺序且不含 KBS 签名的请求头
     */
    public Map<String, String> headers() {
        requireComplete();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-yumc-client-channel", channel);
        headers.put("x-yumc-client-deviceid", deviceId);
        headers.put("x-yumc-client-citycode", cityCode);
        headers.put("x-yumc-client-usercode", userCode);
        headers.put("rcsdcid", rcsdcid);
        headers.put("rcsav", rcsav);
        headers.put("user-agent", userAgent);
        return headers;
    }

    private static void required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void nonNull(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
