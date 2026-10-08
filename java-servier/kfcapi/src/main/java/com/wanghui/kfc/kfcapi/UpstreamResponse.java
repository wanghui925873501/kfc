package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpHeaders;

/** 保存上游 JSON 正文及会话编排所需的响应头。 */
@Data
public class UpstreamResponse {
    /** 供传输层逐项设置响应数据。 */
    public UpstreamResponse() { }

    /** 上游响应的 HTTP 状态码。 */
    private int statusCode;
    /** 上游响应头的只读副本。 */
    @ToString.Exclude
    private HttpHeaders headers;
    /** 上游 JSON 响应正文。 */
    @ToString.Exclude
    private JsonNode body;
}
