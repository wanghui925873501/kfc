package com.wanghui.kfc.phhsapi.apploginphdappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.ToString;

/** PHHS APK 声明的 {@code startCaptcha} 响应。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhhsStartCaptchaVo {
    /** 供 JSON 框架创建响应对象。 */
    public PhhsStartCaptchaVo() { }

    /** 上游业务码。 */
    private Integer errCode;
    /** 验证注册数据，包含一次性极验挑战字段。 */
    @ToString.Exclude
    private JsonNode data;
}
