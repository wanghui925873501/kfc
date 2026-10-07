package com.wanghui.kfc.kfcapi.apploginkfcappcn.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/** Reqable 6374 已确认的验证码登录响应概要；上游 token 只供后端使用。 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginBySmsCodeVo {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public LoginBySmsCodeVo() { }

    /** 上游业务状态码。 */
    private Integer errCode;
    /** 上游扩展错误码。 */
    private String errorCode;
    /** 上游响应中声明的加密字段。 */
    private List<String> encodeList;
    /** 登录会员数据。 */
    private LoginData data;

    /** 登录响应中的品牌数据容器。 */
    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData {
        /** 供 JSON 框架或调用方逐项设置字段。 */
        public LoginData() { }

        /** 主品牌会员数据。 */
        private MainBrandData mainBrandData;
    }

    /** 后端建立登录会话所需的主品牌会员字段。 */
    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MainBrandData {
        /** 供 JSON 框架或调用方逐项设置字段。 */
        public MainBrandData() { }

        /** 上游登录票据，不应返回给 UniApp。 */
        @ToString.Exclude
        private String token;
        /** 上游用户编码。 */
        private String userCode;
        /** 上游会员标识。 */
        private String muid;
        /** 上游身份标识。 */
        private String suid;
        /** 是否已有会员身份。 */
        private Boolean isMember;
    }
}
