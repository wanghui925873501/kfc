package com.wanghui.kfc.basicapi.captcha3;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 跨品牌共用的本机小辉极验三代服务配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "basic.captcha3")
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
