package com.wanghui.kfc.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 单体后端启动入口，扫描业务、数据库和上游接口模块。 */
@SpringBootApplication(scanBasePackages = "com.wanghui.kfc")
@MapperScan("com.wanghui.kfc.db.mapper")
public class ServerApplication {
    /** 供 Spring Boot 创建应用入口。 */
    public ServerApplication() { }

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
