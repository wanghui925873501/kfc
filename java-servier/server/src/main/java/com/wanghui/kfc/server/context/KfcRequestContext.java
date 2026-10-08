package com.wanghui.kfc.server.context;

import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import lombok.Data;
import lombok.ToString;

/** 保存一次内部 HTTP 请求对应的 KFC 用户、token 和设备上下文。 */
@Data
public class KfcRequestContext {
    /** 供上下文加载服务逐项设置字段。 */
    public KfcRequestContext() { }

    /** 当前请求携带的手机号。 */
    @ToString.Exclude
    private String phone;
    /** 当前手机号对应的完整 {@code kfc_user} 数据。 */
    @ToString.Exclude
    private KfcUser kfcUser;
    /** 已核对明密文一致、可供后续 KFC 接口使用的 token。 */
    @ToString.Exclude
    private String upstreamToken;
    /** 当前手机号绑定的完整安装设备数据。 */
    @ToString.Exclude
    private KfcInstallation installation;
    /** 当前手机号对应的客户端档案和风控会话数据。 */
    @ToString.Exclude
    private KfcPhoneInstallation phoneInstallation;
}
