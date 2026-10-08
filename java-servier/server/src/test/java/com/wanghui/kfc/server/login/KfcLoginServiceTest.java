package com.wanghui.kfc.server.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.wanghui.kfc.server.identity.IssuedKfcSession;
import com.wanghui.kfc.server.identity.KfcIdentityService;
import com.wanghui.kfc.server.identity.KfcPhoneDeviceService;
import com.wanghui.kfc.server.identity.KfcSessionProperties;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** 核验正式登录编排的分支边界，所有 KFC 调用均为 Mock。 */
class KfcLoginServiceTest {
    @Test
    void reusesStoredTokenForBothEndpointsWithoutCallingKfc() {
        KfcUserService users = mock(KfcUserService.class);
        AppUserService appUsers = mock(AppUserService.class);
        KfcPhoneDeviceService devices = mock(KfcPhoneDeviceService.class);
        AppLoginCaptchaService captcha = mock(AppLoginCaptchaService.class);
        KfcIdentityService identity = mock(KfcIdentityService.class);
        KfcLoginTriggerGuard guard = immediateGuard();
        KfcUser stored = storedUser();
        KfcPhoneInstallation binding = binding();
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(stored);
        when(devices.findExisting("13800000000")).thenReturn(binding);
        when(identity.issueStoredSession(stored, "installation-id"))
                .thenReturn(new IssuedKfcSession("local-session", 7L));
        KfcLoginService service = service(users, appUsers, devices, captcha, identity, guard);

        KfcLoginResult send = service.sendSmsCode("13800000000");
        KfcLoginResult login = service.loginBySmsCode("13800000000", "123456");

        assertThat(send.isLoggedIn()).isTrue();
        assertThat(send.isSmsSent()).isFalse();
        assertThat(send.isReusedStoredToken()).isTrue();
        assertThat(login.isLoggedIn()).isTrue();
        assertThat(login.isReusedStoredToken()).isTrue();
        assertThat(login.getSessionId()).isEqualTo("local-session");
        verifyNoInteractions(captcha);
    }

    @Test
    void sendsSmsExactlyOnceWhenNoStoredLoginExists() {
        KfcUserService users = mock(KfcUserService.class);
        AppUserService appUsers = mock(AppUserService.class);
        KfcPhoneDeviceService devices = mock(KfcPhoneDeviceService.class);
        AppLoginCaptchaService captcha = mock(AppLoginCaptchaService.class);
        KfcIdentityService identity = mock(KfcIdentityService.class);
        KfcLoginTriggerGuard guard = immediateGuard();
        KfcPhoneInstallation binding = binding();
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(null);
        when(devices.getOrCreate(eq("13800000000"), anyString(), anyString(), anyString()))
                .thenReturn(binding);
        when(devices.loginContext(binding, "")).thenReturn(new AppLoginContext());
        SendSmsCodeVo success = new SendSmsCodeVo();
        success.setErrCode(0);
        when(captcha.sendSmsCode(any())).thenReturn(success);
        KfcLoginService service = service(users, appUsers, devices, captcha, identity, guard);

        KfcLoginResult result = service.sendSmsCode("13800000000");

        assertThat(result.isSmsSent()).isTrue();
        assertThat(result.isLoggedIn()).isFalse();
        verify(guard).claimSmsCooldown(anyString());
        verify(captcha).sendSmsCode(any(SendSmsCodeParam.class));
        verify(identity, never()).issueStoredSession(any(), anyString());
    }

    @Test
    void logsInOncePersistsIdentityAndReturnsOnlyLocalSession() {
        KfcUserService users = mock(KfcUserService.class);
        AppUserService appUsers = mock(AppUserService.class);
        KfcPhoneDeviceService devices = mock(KfcPhoneDeviceService.class);
        AppLoginCaptchaService captcha = mock(AppLoginCaptchaService.class);
        KfcIdentityService identity = mock(KfcIdentityService.class);
        KfcLoginTriggerGuard guard = immediateGuard();
        KfcPhoneInstallation binding = binding();
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(null);
        when(devices.getOrCreate(eq("13800000000"), anyString(), anyString(), anyString()))
                .thenReturn(binding);
        when(devices.loginContext(binding, "")).thenReturn(new AppLoginContext());
        LoginBySmsCodeVo.MainBrandData member = new LoginBySmsCodeVo.MainBrandData(
                "upstream-token", "upstream-user", null, null, true);
        LoginBySmsCodeVo response = new LoginBySmsCodeVo(0, null, null,
                new LoginBySmsCodeVo.LoginData(member));
        when(captcha.loginBySmsCode(any())).thenReturn(response);
        when(appUsers.findByPhoneHash(anyString())).thenReturn(null);
        when(appUsers.save(any())).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(11L);
            return true;
        });
        when(identity.recordLoginSuccess(11L, "installation-id", "13800000000", response))
                .thenReturn(new IssuedKfcSession("local-session", 7L));
        KfcLoginService service = service(users, appUsers, devices, captcha, identity, guard);

        KfcLoginResult result = service.loginBySmsCode("13800000000", "123456");

        assertThat(result.isLoggedIn()).isTrue();
        assertThat(result.isReusedStoredToken()).isFalse();
        assertThat(result.getSessionId()).isEqualTo("local-session");
        assertThat(result.toString()).doesNotContain("upstream-token");
        verify(guard).claimLoginCooldown(anyString());
        verify(captcha).loginBySmsCode(any(LoginBySmsCodeParam.class));
        verify(identity).recordLoginSuccess(11L, "installation-id", "13800000000", response);
    }

    @Test
    void stopsOnIncompleteStoredTokenWithoutCallingKfc() {
        KfcUserService users = mock(KfcUserService.class);
        KfcUser stored = storedUser();
        stored.setTokenCiphertext(null);
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(stored);
        AppLoginCaptchaService captcha = mock(AppLoginCaptchaService.class);
        KfcLoginService service = service(users, mock(AppUserService.class),
                mock(KfcPhoneDeviceService.class), captcha, mock(KfcIdentityService.class), immediateGuard());

        assertThatThrownBy(() -> service.sendSmsCode("13800000000"))
                .isInstanceOf(KfcLoginException.class)
                .extracting("code").isEqualTo("LOGIN_STATE_CORRUPTED");
        verifyNoInteractions(captcha);
    }

    @SuppressWarnings("unchecked")
    private KfcLoginTriggerGuard immediateGuard() {
        KfcLoginTriggerGuard guard = mock(KfcLoginTriggerGuard.class);
        when(guard.withPhoneGate(anyString(), any())).thenAnswer(invocation ->
                ((Supplier<KfcLoginResult>) invocation.getArgument(1)).get());
        return guard;
    }

    private KfcLoginService service(KfcUserService users, AppUserService appUsers,
                                    KfcPhoneDeviceService devices, AppLoginCaptchaService captcha,
                                    KfcIdentityService identity, KfcLoginTriggerGuard guard) {
        KfcLoginClientProperties client = new KfcLoginClientProperties();
        KfcSessionProperties session = new KfcSessionProperties();
        session.setTtl(Duration.ofHours(8));
        return new KfcLoginService(users, appUsers, devices, captcha, identity, guard, client, session,
                new KfcRequestContextHolder());
    }

    private KfcUser storedUser() {
        KfcUser user = new KfcUser();
        user.setId(7L);
        user.setAppUserId(11L);
        user.setPhonePlain("13800000000");
        user.setTokenPlain("upstream-token");
        user.setTokenCiphertext("encrypted-token");
        return user;
    }

    private KfcPhoneInstallation binding() {
        KfcPhoneInstallation value = new KfcPhoneInstallation();
        value.setId(9L);
        value.setInstallationId("installation-id");
        return value;
    }
}
