package com.wanghui.kfc.server.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import com.wanghui.kfc.db.entity.AppUser;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.AppUserService;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KfcIdentityServiceTest {
    @Test
    void createsInstallationBeforeLoginWithoutInventingSdkIdentifiers() {
        KfcInstallationService installations = mock(KfcInstallationService.class);
        when(installations.save(any())).thenAnswer(invocation -> {
            KfcInstallation value = invocation.getArgument(0);
            value.setId(5L);
            return true;
        });
        KfcIdentityService service = new KfcIdentityService(installations, mock(KfcUserLinkService.class),
                mock(KfcUserService.class), mock(KfcSessionStore.class), mock(KfcSessionCipher.class));

        KfcInstallation created = service.registerInstallation("own-device-id", "android", "1.0");
        assertThat(created.getInstallationId()).isNotBlank();
        assertThat(created.getDeviceId()).isEqualTo("own-device-id");
        assertThat(created.getTdid()).isNull();
        assertThat(created.getJpushRegId()).isNull();
    }

    @Test
    void linksOnlySuccessfulLoginAndResolvesBoundDevice() {
        KfcInstallationService installations = mock(KfcInstallationService.class);
        KfcUserLinkService userLink = mock(KfcUserLinkService.class);
        KfcUserService users = mock(KfcUserService.class);
        KfcSessionStore sessions = mock(KfcSessionStore.class);
        KfcSessionCipher cipher = mock(KfcSessionCipher.class);
        KfcIdentityService service = new KfcIdentityService(installations, userLink, users, sessions, cipher);
        KfcInstallation installation = new KfcInstallation();
        installation.setId(5L);
        installation.setInstallationId("own-installation-id");
        installation.setDeviceId("own-device-id");
        installation.setPlatform("android");
        when(installations.findByInstallationId("own-installation-id")).thenReturn(installation);
        KfcUser user = new KfcUser();
        user.setId(7L);
        user.setAppUserId(1L);
        user.setUserCode("sample-user-code");
        user.setTokenPlain("sample-upstream-token");
        user.setTokenCiphertext("encrypted-token");
        when(userLink.bind(eq(1L), eq("test-phone"), any())).thenReturn(user);
        when(cipher.decrypt("encrypted-token"))
                .thenReturn("sample-upstream-token".getBytes(StandardCharsets.UTF_8));
        when(sessions.issue(any())).thenReturn("local-session-id");
        LoginBySmsCodeVo.MainBrandData member = new LoginBySmsCodeVo.MainBrandData(
                "sample-upstream-token", "sample-user-code", null, null, true);
        LoginBySmsCodeVo response = new LoginBySmsCodeVo(0, null, null,
                new LoginBySmsCodeVo.LoginData(member));

        IssuedKfcSession issued = service.recordLoginSuccess(1L, "own-installation-id", "test-phone", response);
        assertThat(issued.getSessionId()).isEqualTo("local-session-id");
        assertThat(issued.getKfcUserId()).isEqualTo(7L);
        when(sessions.resolve("local-session-id"))
                .thenReturn(Optional.of(new KfcSessionContext(1L, 7L, 5L)));
        when(users.getById(7L)).thenReturn(user);
        assertThat(service.resolveUpstreamContext("local-session-id", "own-installation-id"))
                .get().extracting(KfcUpstreamContext::getDeviceId).isEqualTo("own-device-id");

        LoginBySmsCodeVo failed = new LoginBySmsCodeVo(1, null, null,
                new LoginBySmsCodeVo.LoginData(member));
        assertThatThrownBy(() -> service.recordLoginSuccess(1L, "own-installation-id", "test-phone", failed))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userLink).bind(eq(1L), eq("test-phone"), eq(member));
        verify(sessions, never()).revoke("local-session-id");
    }

    @Test
    void rejectsLinkingOneUpstreamAccountToAnotherLocalUser() {
        AppUserService appUsers = mock(AppUserService.class);
        KfcUserService users = mock(KfcUserService.class);
        when(appUsers.getById(2L)).thenReturn(new AppUser());
        KfcUser existing = new KfcUser();
        existing.setId(7L);
        existing.setAppUserId(1L);
        when(users.findByBrandAndUserCode("KFC", "sample-user-code")).thenReturn(existing);
        KfcUserLinkService service = new KfcUserLinkService(appUsers, users,
                mock(AppLoginCrypto.class), mock(KfcSessionCipher.class));
        LoginBySmsCodeVo.MainBrandData member = new LoginBySmsCodeVo.MainBrandData(
                "sample-upstream-token", "sample-user-code", null, null, true);

        assertThatThrownBy(() -> service.bind(2L, "test-phone", member))
                .isInstanceOf(IllegalStateException.class);
        verify(users, never()).updateById(any());
    }

    @Test
    void refusesToReuseSessionWithAnotherInstallation() {
        KfcInstallationService installations = mock(KfcInstallationService.class);
        KfcUserService users = mock(KfcUserService.class);
        KfcSessionStore sessions = mock(KfcSessionStore.class);
        KfcIdentityService service = new KfcIdentityService(installations, mock(KfcUserLinkService.class),
                users, sessions, mock(KfcSessionCipher.class));
        KfcInstallation otherInstallation = new KfcInstallation();
        otherInstallation.setId(9L);
        when(installations.findByInstallationId("different-installation")).thenReturn(otherInstallation);
        when(sessions.resolve("local-session-id"))
                .thenReturn(Optional.of(new KfcSessionContext(1L, 7L, 5L)));
        KfcUser user = new KfcUser();
        user.setId(7L);
        user.setAppUserId(1L);
        when(users.getById(7L)).thenReturn(user);

        assertThat(service.resolveUpstreamContext("local-session-id", "different-installation")).isEmpty();
        verify(sessions).revoke("local-session-id");
    }

    @Test
    void persistsPlainAndEncryptedPhoneAndTokenTogether() {
        AppUserService appUsers = mock(AppUserService.class);
        KfcUserService users = mock(KfcUserService.class);
        when(appUsers.getById(1L)).thenReturn(new AppUser());
        when(users.save(any())).thenAnswer(invocation -> {
            KfcUser value = invocation.getArgument(0);
            value.setId(7L);
            return true;
        });
        AppLoginProperties loginProperties = new AppLoginProperties();
        loginProperties.setDesKey("12345678");
        AppLoginCrypto phoneCrypto = new AppLoginCrypto(loginProperties);
        KfcSessionProperties sessionProperties = new KfcSessionProperties();
        sessionProperties.setKeyBase64(Base64.getEncoder().encodeToString(new byte[32]));
        KfcSessionCipher tokenCipher = new KfcSessionCipher(sessionProperties);
        KfcUserLinkService service = new KfcUserLinkService(appUsers, users, phoneCrypto, tokenCipher);
        LoginBySmsCodeVo.MainBrandData member = new LoginBySmsCodeVo.MainBrandData(
                "sample-upstream-token", "sample-user-code", null, null, true);

        service.bind(1L, "test-phone", member);

        ArgumentCaptor<KfcUser> saved = ArgumentCaptor.forClass(KfcUser.class);
        verify(users).save(saved.capture());
        KfcUser row = saved.getValue();
        assertThat(row.getPhonePlain()).isEqualTo("test-phone");
        assertThat(row.getPhoneCiphertext()).isEqualTo(phoneCrypto.encrypt("test-phone"));
        assertThat(row.getTokenPlain()).isEqualTo("sample-upstream-token");
        assertThat(new String(tokenCipher.decrypt(row.getTokenCiphertext()), StandardCharsets.UTF_8))
                .isEqualTo(row.getTokenPlain());
        assertThat(row.toString()).doesNotContain("test-phone", "sample-upstream-token");
    }

    @Test
    void revokesSessionWhenStoredTokenPairDoesNotMatch() {
        KfcInstallationService installations = mock(KfcInstallationService.class);
        KfcUserService users = mock(KfcUserService.class);
        KfcSessionStore sessions = mock(KfcSessionStore.class);
        KfcSessionCipher cipher = mock(KfcSessionCipher.class);
        KfcIdentityService service = new KfcIdentityService(installations, mock(KfcUserLinkService.class),
                users, sessions, cipher);
        KfcInstallation installation = new KfcInstallation();
        installation.setId(5L);
        when(installations.findByInstallationId("own-installation-id")).thenReturn(installation);
        when(sessions.resolve("local-session-id"))
                .thenReturn(Optional.of(new KfcSessionContext(1L, 7L, 5L)));
        KfcUser user = new KfcUser();
        user.setAppUserId(1L);
        user.setTokenPlain("old-token");
        user.setTokenCiphertext("encrypted-token");
        when(users.getById(7L)).thenReturn(user);
        when(cipher.decrypt("encrypted-token")).thenReturn("other-token".getBytes(StandardCharsets.UTF_8));

        assertThat(service.resolveUpstreamContext("local-session-id", "own-installation-id")).isEmpty();
        verify(sessions).revoke("local-session-id");
    }
}
