package com.wanghui.kfc.alipay.application;

import lombok.AllArgsConstructor;
import lombok.Data;

/** KFC 自有后端返回的最小订单状态快照。 */
@Data
@AllArgsConstructor
public class KfcOrderSnapshot {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public KfcOrderSnapshot() { }

    /** KFC 订单号。 */
    private String orderId;
    /** KFC 状态码，例如 {@code 102} 或 {@code 504}。 */
    private String status;
    /** KFC 状态名称。 */
    private String statusName;
}
