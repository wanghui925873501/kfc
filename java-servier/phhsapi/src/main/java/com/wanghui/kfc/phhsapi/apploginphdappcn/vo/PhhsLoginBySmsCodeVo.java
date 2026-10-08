package com.wanghui.kfc.phhsapi.apploginphdappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** 表示 Reqable 会话 497 已验证的必胜客短信登录响应。 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhhsLoginBySmsCodeVo {
    /** 供 JSON 框架创建响应对象。 */
    public PhhsLoginBySmsCodeVo() { }

    /** 上游业务状态码，成功样本为 0。 */
    private Integer errCode;
    /** 上游扩展错误码。 */
    private String errorCode;
    /** 风险响应的一次性数据。 */
    @ToString.Exclude
    private JsonNode errData;
    /** 响应中声明的加密字段。 */
    private List<String> encodeList;
    /** 登录成功后的主品牌数据。 */
    private LoginData data;

    /**
     * 判断是否进入 APK 中的图形验证分支。
     * @return 风险码为 5910060 或 5910061 时返回 true
     */
    public boolean requiresVerification() {
        return Integer.valueOf(5910060).equals(errCode) || Integer.valueOf(5910061).equals(errCode);
    }

    /** 登录响应的数据容器。 */
    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData {
        /** 供 JSON 框架创建数据容器。 */
        public LoginData() { }

        /** PHHS 主品牌会员信息。 */
        private MainBrandData mainBrandData;
    }

    /** 登录成功后 Java 调用方需要使用的最小 PHHS 会员字段。 */
    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MainBrandData {
        /** 供 JSON 框架创建会员对象。 */
        public MainBrandData() { }

        /** 上游登录票据，不得直接返回前端。 */
        @ToString.Exclude
        private String token;
        /** PHHS 用户编码。 */
        private String userCode;
        /** PHHS 会员标识。 */
        private String muid;
        /** 百胜统一身份标识。 */
        private String suid;
        /** 是否已有会员身份；首次手机号登录可自动注册。 */
        private Boolean isMember;
        /** 响应中的加密手机号，不写入普通日志。 */
        @ToString.Exclude
        private String phone;
        /** 响应中的品牌手机号字段，不写入普通日志。 */
        @ToString.Exclude
        private String bPhone;
        /** 百胜统一用户概要，不写入普通日志。 */
        @ToString.Exclude
        private JsonNode su;
    }
}
