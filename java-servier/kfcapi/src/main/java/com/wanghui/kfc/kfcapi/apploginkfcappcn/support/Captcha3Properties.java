package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本机小辉极验三代服务的显式接入配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "kfc.captcha3")
public class Captcha3Properties {
    /** 供 Spring 绑定配置。 */
    public Captcha3Properties() { }

    /** 默认关闭在线识别，需部署方显式启用。 */
    private boolean enabled;
    /** 本机或受控局域网中的固定服务 URL。 */
    private String url = "http://127.0.0.1:16254/captcha3";
    /** 局域网监听时的访问密钥，不能写入日志。 */
    @ToString.Exclude
    private String apiKey;
}
