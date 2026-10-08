package com.wanghui.kfc.kfcapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import org.springframework.stereotype.Component;

/** 登录上游的固定接口封装，不直接向 uni-app 暴露。 */
@Component
public class LoginApi {
    /** 向登录上游固定接口发送请求的传输层。 */
    private final UpstreamGateway gateway;

    /**
     * 创建登录上游封装。
     * @param gateway 固定域名与路径的上游传输层
     */
    public LoginApi(UpstreamGateway gateway) { this.gateway = gateway; }

    /**
     * 校验上游 token，POST {@code /KBS/api/user/token/valid}。
     * APK 证据：{@code 01249.js} 的方法和路径、{@code 01290.js} 的前缀；参数和鉴权仍待联调。
     *
     * @param request 上游校验参数，含待校验的 token
     * @return 上游校验结果 JSON
     */
    public JsonNode validateToken(JsonNode request) {
        return gateway.post(KfcUpstream.LOGIN, "/KBS/api/user/token/valid", request);
    }
}
