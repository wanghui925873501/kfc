package com.wanghui.kfc.server.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 后端调用后续 KFC 接口时使用的内部上下文，不直接返回给 UniApp。 */
@Data
@AllArgsConstructor
public class KfcUpstreamContext {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public KfcUpstreamContext() { }

    /** 上游用户编码。 */
    private String userCode;
    /** 从数据库密文解出的上游登录票据。 */
    @ToString.Exclude
    private String upstreamToken;
    /** 当前应用安装的设备标识。 */
    @ToString.Exclude
    private String deviceId;
    /** SDK 返回的设备追踪标识，可能为空。 */
    @ToString.Exclude
    private String tdid;
    /** 推送 SDK 注册标识，可能为空。 */
    @ToString.Exclude
    private String jpushRegId;
    /** 当前应用平台。 */
    private String platform;
    /** 当前应用版本，可能为空。 */
    private String appVersion;
}
