package com.wanghui.kfc.kfcapi.apploginkfcappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 发送验证码的上游响应；成功响应结构仍待验证，风险响应由手动测试确认。 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendSmsCodeVo {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public SendSmsCodeVo() { }

    /** 上游业务状态码，若响应不含此字段则为空。 */
    private Integer errCode;
    /** 上游扩展错误码，若响应不含此字段则为空。 */
    private String errorCode;
    /** 上游错误说明，若响应不含此字段则为空。 */
    private String errMsg;
    /** 风险验证扩展数据，可能包含一次性事件标识，不应写入业务日志。 */
    @ToString.Exclude
    private JsonNode errData;
    /** 尚未确认结构的响应数据。 */
    private JsonNode data;

    /**
     * 判断上游是否要求用户完成交互式验证。
     * @return 需要人工验证时为 true
     */
    public boolean requiresHumanVerification() {
        return Integer.valueOf(5910060).equals(errCode) || Integer.valueOf(5910061).equals(errCode);
    }
}
