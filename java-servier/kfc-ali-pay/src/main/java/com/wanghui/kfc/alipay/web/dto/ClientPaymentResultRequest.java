package com.wanghui.kfc.alipay.web.dto;

import com.wanghui.kfc.alipay.domain.ClientPaymentOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/** UniApp 上报的非权威支付结果。 */
@Data
@AllArgsConstructor
public class ClientPaymentResultRequest {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public ClientPaymentResultRequest() { }

    /** 客户端结果。 */
    @NotNull
    private ClientPaymentOutcome outcome;
    /** 非敏感说明，不得包含完整支付串或登录票据。 */
    private String message;
}
