package com.wanghui.kfc.alipay;

import com.wanghui.kfc.alipay.config.KfcAliPayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KFC 支付宝支付编排服务入口。
 */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(KfcAliPayProperties.class)
public class KfcAliPayApplication {

    /**
     * 创建应用入口实例。
     */
    public KfcAliPayApplication() {
        // 由 Spring Boot 管理应用入口实例。
    }

    /**
     * 启动独立支付编排服务。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KfcAliPayApplication.class, args);
    }
}
