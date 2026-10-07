package com.wanghui.kfc.server.identity;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Redis 中保存的后端登录会话引用；上游 token 持久化在数据库中。 */
@Data
@AllArgsConstructor
public class KfcSessionContext {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public KfcSessionContext() { }

    /** 本地用户主键。 */
    private Long appUserId;
    /** KFC 用户映射主键。 */
    private Long kfcUserId;
    /** 安装记录数据库主键。 */
    private Long installationDbId;
}
