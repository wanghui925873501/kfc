package com.wanghui.kfc.server.controller.login;

import com.wanghui.kfc.common.ApiResponse;
import com.wanghui.kfc.server.controller.login.dto.LoginNextAction;
import com.wanghui.kfc.server.controller.login.dto.LoginSessionDto;
import com.wanghui.kfc.server.controller.login.param.SendSmsCodeParam;
import com.wanghui.kfc.server.controller.login.param.SmsCodeLoginParam;
import com.wanghui.kfc.server.controller.login.vo.SendSmsCodeVo;
import com.wanghui.kfc.server.controller.login.vo.SmsCodeLoginVo;
import com.wanghui.kfc.server.login.KfcLoginResult;
import com.wanghui.kfc.server.login.KfcLoginService;
import com.wanghui.kfc.server.web.annotation.KfcLoginOptional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供给前端的手机号短信发送和验证码登录接口。 */
@RestController
@RequestMapping(value = "/api/v1/login", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "登录接口", description = "手机号短信登录；KFC token 始终只保存在后端")
@KfcLoginOptional
public class LoginController {
    /** 正式登录业务编排服务。 */
    private final KfcLoginService loginService;

    /**
     * 创建登录控制器。
     *
     * @param loginService 正式登录业务编排服务
     */
    public LoginController(KfcLoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 未登录时请求 KFC 发送验证码；已有 token 时直接返回本地会话。
     *
     * @param param 授权手机号
     * @return 是否发码以及前端下一步操作
     */
    @PostMapping(value = "/sms-code/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "发送短信验证码", description = "先检查 kfc_user；已有 token 时不会调用 KFC。")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                description = "短信已确认发送，或已复用本地登录态"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                description = "手机号格式不正确"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                description = "本地登录数据不一致"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429",
                description = "请求处理中或发码过于频繁"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502",
                description = "KFC 未确认发送成功")
    })
    public ApiResponse<SendSmsCodeVo> sendSmsCode(@Valid @RequestBody SendSmsCodeParam param) {
        KfcLoginResult result = loginService.sendSmsCode(param.getPhone());
        SendSmsCodeVo response = new SendSmsCodeVo();
        response.setLoggedIn(result.isLoggedIn());
        response.setSmsSent(result.isSmsSent());
        response.setNextAction(result.isLoggedIn()
                ? LoginNextAction.USE_SESSION : LoginNextAction.ENTER_SMS_CODE);
        response.setSession(session(result));
        return ApiResponse.ok(response);
    }

    /**
     * 未登录时使用当次验证码请求 KFC 登录；已有 token 时直接返回本地会话。
     *
     * @param param 授权手机号和当次短信验证码
     * @return 不含 KFC token 的本地会话
     */
    @PostMapping(value = "/sms-code/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "手机号验证码登录", description = "先检查 kfc_user；已有 token 时忽略验证码且不调用 KFC。")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                description = "登录成功或已复用本地登录态"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                description = "手机号或验证码格式不正确"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                description = "本地登录数据不一致"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
                description = "KFC 未确认验证码登录成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429",
                description = "请求处理中或提交过于频繁")
    })
    public ApiResponse<SmsCodeLoginVo> loginBySmsCode(@Valid @RequestBody SmsCodeLoginParam param) {
        KfcLoginResult result = loginService.loginBySmsCode(param.getPhone(), param.getSmsCode());
        SmsCodeLoginVo response = new SmsCodeLoginVo();
        response.setReusedStoredToken(result.isReusedStoredToken());
        response.setSession(session(result));
        return ApiResponse.ok(response);
    }

    private LoginSessionDto session(KfcLoginResult result) {
        if (!result.isLoggedIn()) return null;
        return new LoginSessionDto(result.getSessionId(), result.getInstallationId(),
                result.getExpiresInSeconds());
    }
}
