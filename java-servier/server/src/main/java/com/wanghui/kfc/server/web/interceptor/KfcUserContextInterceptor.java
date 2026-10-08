package com.wanghui.kfc.server.web.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import com.wanghui.kfc.server.context.KfcRequestContextService;
import com.wanghui.kfc.server.login.KfcLoginException;
import com.wanghui.kfc.server.web.annotation.KfcLoginOptional;
import com.wanghui.kfc.server.web.filter.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 通过每次请求携带的手机号加载 KFC 用户、token 与设备上下文。 */
@Component
public class KfcUserContextInterceptor implements HandlerInterceptor {
    /** 手机号上下文加载服务。 */
    private final KfcRequestContextService contextService;
    /** 当前 HTTP 请求的上下文容器。 */
    private final KfcRequestContextHolder contextHolder;

    /**
     * 创建手机号上下文拦截器。
     *
     * @param contextService 手机号上下文加载服务
     * @param contextHolder 当前请求上下文容器
     */
    public KfcUserContextInterceptor(KfcRequestContextService contextService,
                                     KfcRequestContextHolder contextHolder) {
        this.contextService = contextService;
        this.contextHolder = contextHolder;
    }

    /**
     * 从请求顶层参数读取手机号并加载本地登录上下文。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler Spring MVC 处理器
     * @return 校验通过时为 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        String phone = extractPhone(request);
        if (StrUtil.isBlank(phone)) {
            throw new KfcLoginException("PHONE_REQUIRED", "请求必须携带手机号", HttpStatus.BAD_REQUEST);
        }
        if (!phone.matches("1[0-9]{10}")) {
            throw new KfcLoginException("INVALID_PHONE", "手机号格式不正确", HttpStatus.BAD_REQUEST);
        }
        Optional<KfcRequestContext> loaded = contextService.load(phone);
        loaded.ifPresent(contextHolder::set);
        if (loaded.isEmpty() && !loginOptional(method)) {
            throw new KfcLoginException("LOGIN_REQUIRED", "该手机号尚未登录",
                    HttpStatus.UNAUTHORIZED);
        }
        return true;
    }

    /**
     * 请求完成后清除用户、token 和设备引用。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler Spring MVC 处理器
     * @param ex 请求处理异常，正常完成时为空
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        contextHolder.clear();
    }

    private String extractPhone(HttpServletRequest request) {
        String parameter = request.getParameter("phone");
        if (StrUtil.isNotBlank(parameter)) return parameter;
        if (!(request instanceof CachedBodyHttpServletRequest cached)) return null;
        byte[] body = cached.getCachedBody();
        if (body.length == 0) return null;
        try {
            JSONObject json = JSON.parseObject(body);
            return json == null ? null : json.getString("phone");
        } catch (JSONException e) {
            throw new KfcLoginException("INVALID_REQUEST", "请求体必须是合法 JSON",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean loginOptional(HandlerMethod method) {
        return AnnotatedElementUtils.hasAnnotation(method.getMethod(), KfcLoginOptional.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), KfcLoginOptional.class);
    }
}
