package com.wanghui.kfc.server.rnorder;

import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import lombok.Data;
import lombok.ToString;

/** 保存服务端内部的 RN 点餐流程归属和上游会话。 */
@Data
public class RnOrderFlowState {
    /** 供 JSON 框架恢复加密的流程状态。 */
    public RnOrderFlowState() { }

    /** 流程所属手机号的 SHA-256，用于阻止跨账号复用。 */
    @ToString.Exclude
    private String phoneHash;
    /** 仅服务端保存的上游 sessionId、Cookie 和路由单元。 */
    @ToString.Exclude
    private RnOrderSession session;
}
