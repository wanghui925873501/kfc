package com.wanghui.kfc.server.context;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** 在单次 HTTP 请求内保存已由拦截器验证的手机号业务上下文。 */
@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class KfcRequestContextHolder {
    /** 当前请求的 KFC 上下文；未登录时为空。 */
    private KfcRequestContext context;

    /** 供 Spring 创建请求级上下文容器。 */
    public KfcRequestContextHolder() { }

    /**
     * 保存当前请求已经验证的 KFC 上下文。
     *
     * @param context 已验证的手机号、用户、token 和安装上下文
     */
    public void set(KfcRequestContext context) {
        this.context = context;
    }

    /**
     * 读取当前请求上下文。
     *
     * @return 已登录上下文；当前手机号未登录时为空
     */
    public KfcRequestContext get() {
        return context;
    }

    /**
     * 读取必须存在的登录上下文。
     *
     * @return 已登录上下文
     * @throws IllegalStateException 当前请求没有登录上下文时
     */
    public KfcRequestContext require() {
        if (context == null) throw new IllegalStateException("KFC request context is missing");
        return context;
    }

    /** 清除当前请求保存的用户、token 和设备引用。 */
    public void clear() {
        context = null;
    }
}
