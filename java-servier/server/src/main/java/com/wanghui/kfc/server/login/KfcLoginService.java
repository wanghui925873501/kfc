package com.wanghui.kfc.server.login;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.wanghui.kfc.db.entity.AppUser;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.AppUserService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.LoginBySmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import com.wanghui.kfc.server.identity.IssuedKfcSession;
import com.wanghui.kfc.server.identity.KfcIdentityService;
import com.wanghui.kfc.server.identity.KfcPhoneDeviceService;
import com.wanghui.kfc.server.identity.KfcSessionProperties;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 为前端编排短信发送、验证码登录和已有 token 的本地会话复用。 */
@Service
public class KfcLoginService {
    /** 固定的 KFC 品牌标识。 */
    private static final String BRAND = "KFC";
    /** KFC 用户映射查询入口。 */
    private final KfcUserService kfcUsers;
    /** 本地应用用户持久化入口。 */
    private final AppUserService appUsers;
    /** 按手机号创建并复用安装上下文的服务。 */
    private final KfcPhoneDeviceService phoneDevices;
    /** KFC 短信登录及验证挑战编排服务。 */
    private final AppLoginCaptchaService captchaLogin;
    /** 本地用户、token 和会话编排服务。 */
    private final KfcIdentityService identity;
    /** 同手机号外部请求的互斥与冷却保护。 */
    private final KfcLoginTriggerGuard triggerGuard;
    /** 后端控制的登录客户端档案。 */
    private final KfcLoginClientProperties clientProperties;
    /** 本地会话有效期配置。 */
    private final KfcSessionProperties sessionProperties;
    /** 当前 HTTP 请求已由拦截器加载的手机号上下文。 */
    private final KfcRequestContextHolder requestContextHolder;

    /**
     * 创建正式的前端登录编排服务。
     *
     * @param kfcUsers KFC 用户映射查询入口
     * @param appUsers 本地应用用户持久化入口
     * @param phoneDevices 手机号安装上下文服务
     * @param captchaLogin KFC 登录挑战编排服务
     * @param identity 本地身份和会话服务
     * @param triggerGuard 同手机号触发保护
     * @param clientProperties 后端控制的客户端档案
     * @param sessionProperties 本地会话有效期配置
     * @param requestContextHolder 当前请求的手机号上下文
     */
    public KfcLoginService(KfcUserService kfcUsers, AppUserService appUsers,
                           KfcPhoneDeviceService phoneDevices, AppLoginCaptchaService captchaLogin,
                           KfcIdentityService identity, KfcLoginTriggerGuard triggerGuard,
                           KfcLoginClientProperties clientProperties,
                           KfcSessionProperties sessionProperties,
                           KfcRequestContextHolder requestContextHolder) {
        this.kfcUsers = kfcUsers;
        this.appUsers = appUsers;
        this.phoneDevices = phoneDevices;
        this.captchaLogin = captchaLogin;
        this.identity = identity;
        this.triggerGuard = triggerGuard;
        this.clientProperties = clientProperties;
        this.sessionProperties = sessionProperties;
        this.requestContextHolder = requestContextHolder;
    }

    /**
     * 已有 token 时直接签发本地会话；否则在单次触发保护内请求 KFC 发送验证码。
     *
     * @param phone 账号持有人提交的手机号
     * @return 脱敏后的发送结果或已有本地会话
     */
    public KfcLoginResult sendSmsCode(String phone) {
        requirePhone(phone);
        KfcRequestContext intercepted = requestContextHolder.get();
        if (intercepted != null) return reuseRequestContext(intercepted, phone);
        String phoneHash = SecureUtil.sha256(phone);
        return triggerGuard.withPhoneGate(phoneHash, () -> {
            KfcUser stored = reusableUser(phone);
            if (stored != null) {
                return reuseStoredLogin(stored, phone);
            }
            triggerGuard.claimSmsCooldown(phoneHash);
            KfcPhoneInstallation binding = loginBinding(phone);
            SendSmsCodeParam upstream = new SendSmsCodeParam();
            upstream.setPhone(phone);
            upstream.setContext(phoneDevices.loginContext(binding, ""));
            SendSmsCodeVo response = captchaLogin.sendSmsCode(upstream);
            if (response == null || response.getErrCode() == null || response.getErrCode() != 0) {
                throw new KfcLoginException("SMS_SEND_REJECTED", "KFC 未确认验证码发送成功",
                        HttpStatus.BAD_GATEWAY);
            }
            KfcLoginResult result = new KfcLoginResult();
            result.setSmsSent(true);
            return result;
        });
    }

    /**
     * 已有 token 时忽略验证码并复用；否则调用 KFC 登录并持久化 token 后签发本地会话。
     *
     * @param phone 账号持有人提交的手机号
     * @param smsCode 当次收到的六位短信验证码
     * @return 不含 KFC token 的本地登录结果
     */
    public KfcLoginResult loginBySmsCode(String phone, String smsCode) {
        requirePhone(phone);
        KfcRequestContext intercepted = requestContextHolder.get();
        if (intercepted != null) return reuseRequestContext(intercepted, phone);
        if (smsCode == null || !smsCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("短信验证码必须是六位数字");
        }
        String phoneHash = SecureUtil.sha256(phone);
        return triggerGuard.withPhoneGate(phoneHash, () -> {
            KfcUser stored = reusableUser(phone);
            if (stored != null) {
                return reuseStoredLogin(stored, phone);
            }
            triggerGuard.claimLoginCooldown(phoneHash);
            KfcPhoneInstallation binding = loginBinding(phone);
            AppLoginContext context = phoneDevices.loginContext(binding, "");
            LoginBySmsCodeParam upstream = new LoginBySmsCodeParam();
            upstream.setPhone(phone);
            upstream.setSmsCode(smsCode);
            upstream.setContext(context);
            LoginBySmsCodeVo response = captchaLogin.loginBySmsCode(upstream);
            if (!successful(response)) {
                throw new KfcLoginException("SMS_CODE_LOGIN_REJECTED", "KFC 未确认验证码登录成功",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            AppUser appUser = findOrCreateAppUser(phoneHash);
            IssuedKfcSession session = identity.recordLoginSuccess(appUser.getId(),
                    binding.getInstallationId(), phone, response);
            return sessionResult(session, binding.getInstallationId(), false);
        });
    }

    private KfcUser reusableUser(String phone) {
        KfcUser user;
        try {
            user = kfcUsers.findByBrandAndPhone(BRAND, phone);
        } catch (IllegalStateException e) {
            throw corruptedState();
        }
        if (user == null) return null;
        boolean plainMissing = StrUtil.isBlank(user.getTokenPlain());
        boolean cipherMissing = StrUtil.isBlank(user.getTokenCiphertext());
        if (plainMissing && cipherMissing) return null;
        if (plainMissing || cipherMissing) throw corruptedState();
        return user;
    }

    private KfcLoginResult reuseStoredLogin(KfcUser user, String phone) {
        KfcPhoneInstallation binding;
        try {
            binding = phoneDevices.findExisting(phone);
            if (binding == null) binding = loginBinding(phone);
            IssuedKfcSession session = identity.issueStoredSession(user, binding.getInstallationId());
            return sessionResult(session, binding.getInstallationId(), true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw corruptedState();
        }
    }

    private KfcLoginResult reuseRequestContext(KfcRequestContext context, String phone) {
        if (!phone.equals(context.getPhone()) || context.getKfcUser() == null
                || context.getInstallation() == null) {
            throw corruptedState();
        }
        try {
            IssuedKfcSession session = identity.issueStoredSession(context.getKfcUser(),
                    context.getInstallation().getInstallationId());
            return sessionResult(session, context.getInstallation().getInstallationId(), true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw corruptedState();
        }
    }

    private KfcPhoneInstallation loginBinding(String phone) {
        return phoneDevices.getOrCreate(phone, clientProperties.getCityCode(),
                clientProperties.getUserAgent(), clientProperties.getAppVersion());
    }

    private AppUser findOrCreateAppUser(String phoneHash) {
        AppUser user = appUsers.findByPhoneHash(phoneHash);
        if (user != null) return user;
        user = new AppUser();
        user.setNickname("KFC用户");
        user.setPhoneHash(phoneHash);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (!appUsers.save(user) || user.getId() == null) {
            throw new KfcLoginException("LOCAL_USER_SAVE_FAILED", "本地用户创建失败",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return user;
    }

    private KfcLoginResult sessionResult(IssuedKfcSession session, String installationId,
                                          boolean reusedStoredToken) {
        KfcLoginResult result = new KfcLoginResult();
        result.setLoggedIn(true);
        result.setReusedStoredToken(reusedStoredToken);
        result.setSessionId(session.getSessionId());
        result.setInstallationId(installationId);
        result.setExpiresInSeconds(sessionProperties.getTtl().toSeconds());
        return result;
    }

    private boolean successful(LoginBySmsCodeVo response) {
        return response != null && response.getErrCode() != null && response.getErrCode() == 0
                && response.getData() != null && response.getData().getMainBrandData() != null
                && StrUtil.isNotBlank(response.getData().getMainBrandData().getToken());
    }

    private KfcLoginException corruptedState() {
        return new KfcLoginException("LOGIN_STATE_CORRUPTED", "本地登录数据不完整，已停止调用 KFC",
                HttpStatus.CONFLICT);
    }

    private void requirePhone(String phone) {
        if (phone == null || !phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("手机号必须是 11 位中国大陆手机号");
        }
    }
}
