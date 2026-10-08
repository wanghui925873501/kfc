package com.wanghui.kfc.kfcapi.apploginkfcappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.ToString;

/** APK 声明的 {@code startCaptcha} 响应，具体结构尚待本域名抓包验证。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartCaptchaVo {
    /** 供 JSON 框架创建响应对象。 */
    public StartCaptchaVo() { }

    /** 上游业务码。 */
    private Integer errCode;
    /** 验证注册数据，可能包含一次性挑战字段。 */
    @ToString.Exclude
    private JsonNode data;
}
