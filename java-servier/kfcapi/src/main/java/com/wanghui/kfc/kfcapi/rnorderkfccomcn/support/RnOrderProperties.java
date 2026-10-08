package com.wanghui.kfc.kfcapi.rnorderkfccomcn.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 保存 2026-10-07 Reqable 样本确认的 React Native 点餐客户端档案。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.rn-order")
public class RnOrderProperties {
    /** 供 Spring 绑定点餐客户端档案。 */
    public RnOrderProperties() { }

    /** 上游入口类型。 */
    private String portalType = "APP";
    /** 上游渠道名称。 */
    private String channelName = "SuperApp";
    /** 上游渠道编号。 */
    private String channelId = "13_2";
    /** 预点餐品牌标识。 */
    private String brand = "KFC_PRE";
    /** 业务线标识。 */
    private String business = "preorder";
    /** RN 点餐客户端版本标识。 */
    private String clientVersion = "v6.921(cd08b0d3)";
    /** RN 点餐前端资源版本。 */
    private String fversion = "20260914";
    /** 点餐协议版本号。 */
    private String versionNum = "5";
    /** 点餐环境标识。 */
    private String env = "ks";
    /** 请求头中的 RN bundle 版本描述。 */
    private String rnBundle = "{\"bundle\":\"kfc-ordering-preorder\","
            + "\"commit\":\"cd08b0d391c07e2fff6c7cc7488d8a537191315e\",\"label\":\"v240\"}";

    /**
     * 确认客户端档案字段完整后返回当前配置。
     *
     * @return 已验证完整的客户端档案
     * @throws IllegalStateException 任一固定协议字段为空时
     */
    public RnOrderProperties requireComplete() {
        if (isBlank(portalType) || isBlank(channelName) || isBlank(channelId) || isBlank(brand)
                || isBlank(business) || isBlank(clientVersion) || isBlank(fversion)
                || isBlank(versionNum) || isBlank(env) || isBlank(rnBundle)) {
            throw new IllegalStateException("RN order client profile is incomplete");
        }
        return this;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
