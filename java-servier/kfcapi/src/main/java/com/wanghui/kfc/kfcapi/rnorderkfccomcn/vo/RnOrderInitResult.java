package com.wanghui.kfc.kfcapi.rnorderkfccomcn.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import lombok.Data;
import lombok.ToString;

/** 返回 RN 点餐初始化正文及后续调用必须复用的内部会话。 */
@Data
public class RnOrderInitResult {
    /** 供客户端逐项设置初始化结果。 */
    public RnOrderInitResult() { }

    /** 上游初始化 JSON 响应。 */
    @ToString.Exclude
    private JsonNode response;
    /** 从响应头和正文建立的点餐会话。 */
    @ToString.Exclude
    private RnOrderSession session;
}
