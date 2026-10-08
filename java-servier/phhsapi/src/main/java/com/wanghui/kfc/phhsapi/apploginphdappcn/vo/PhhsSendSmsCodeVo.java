package com.wanghui.kfc.phhsapi.apploginphdappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 表示必胜客短信发送响应；首份成功抓包缺少响应体，因此保留未知数据节点。 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhhsSendSmsCodeVo {
    /** 供 JSON 框架创建响应对象。 */
    public PhhsSendSmsCodeVo() { }

    /** 上游业务状态码。 */
    private Integer errCode;
    /** 上游扩展错误码。 */
    private String errorCode;
    /** 上游错误说明。 */
    private String errMsg;
    /** 风险验证扩展数据，不写入普通日志。 */
    @ToString.Exclude
    private JsonNode errData;
    /** 尚未完整确认结构的成功数据。 */
    @ToString.Exclude
    private JsonNode data;

    /**
     * 判断上游是否要求交互式验证。
     * @return 风险码为 5910060 或 5910061 时返回 true
     */
    public boolean requiresHumanVerification() {
        return Integer.valueOf(5910060).equals(errCode) || Integer.valueOf(5910061).equals(errCode);
    }
}
