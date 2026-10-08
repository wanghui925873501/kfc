package com.wanghui.kfc.server.config;

import com.wanghui.kfc.server.web.interceptor.KfcUserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册内部 API 的手机号 KFC 上下文拦截器。 */
@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {
    /** 手机号 KFC 上下文拦截器。 */
    private final KfcUserContextInterceptor userContextInterceptor;

    /**
     * 创建 Spring MVC 配置。
     *
     * @param userContextInterceptor 手机号 KFC 上下文拦截器
     */
    public WebMvcConfiguration(KfcUserContextInterceptor userContextInterceptor) {
        this.userContextInterceptor = userContextInterceptor;
    }

    /**
     * 对所有 v1 前端业务接口启用手机号上下文加载。
     *
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor).addPathPatterns("/api/v1/**");
    }
}
