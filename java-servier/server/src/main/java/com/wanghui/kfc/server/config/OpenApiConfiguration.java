package com.wanghui.kfc.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** 声明提供给 UniApp 的服务端 OpenAPI 3 文档基础信息。 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "KFC UniApp 后端接口", version = "v1",
        description = "UniApp 仅访问本后端；KFC 上游 token、签名密钥和设备上下文不会下发。"))
public class OpenApiConfiguration {
    /** 供 Spring 创建 OpenAPI 文档配置。 */
    public OpenApiConfiguration() { }
}
