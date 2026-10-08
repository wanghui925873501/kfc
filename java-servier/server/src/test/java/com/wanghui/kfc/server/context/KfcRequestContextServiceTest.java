package com.wanghui.kfc.server.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.server.identity.KfcPhoneDeviceService;
import com.wanghui.kfc.server.identity.KfcSessionCipher;
import com.wanghui.kfc.server.login.KfcLoginException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 核验手机号请求上下文只接受完整且一致的本地用户、token 与设备数据。 */
class KfcRequestContextServiceTest {
    @Test
    void loadsUserTokenAndDeviceDataForLoggedPhone() {
        KfcUserService users = mock(KfcUserService.class);
        KfcPhoneDeviceService phoneDevices = mock(KfcPhoneDeviceService.class);
        KfcInstallationService installations = mock(KfcInstallationService.class);
        KfcSessionCipher cipher = mock(KfcSessionCipher.class);
        KfcUser user = user();
        KfcPhoneInstallation phoneInstallation = new KfcPhoneInstallation();
        phoneInstallation.setInstallationId("installation-id");
        KfcInstallation installation = new KfcInstallation();
        installation.setId(9L);
        installation.setInstallationId("installation-id");
        installation.setDeviceId("device-id");
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(user);
        when(cipher.decrypt("encrypted-token"))
                .thenReturn("upstream-token".getBytes(StandardCharsets.UTF_8));
        when(phoneDevices.findExisting("13800000000")).thenReturn(phoneInstallation);
        when(installations.findByInstallationId("installation-id")).thenReturn(installation);
        KfcRequestContextService service = new KfcRequestContextService(
                users, phoneDevices, installations, cipher);

        KfcRequestContext context = service.load("13800000000").orElseThrow();

        assertThat(context.getKfcUser()).isSameAs(user);
        assertThat(context.getUpstreamToken()).isEqualTo("upstream-token");
        assertThat(context.getInstallation()).isSameAs(installation);
        assertThat(context.getPhoneInstallation()).isSameAs(phoneInstallation);
        assertThat(context.toString()).doesNotContain("13800000000", "upstream-token", "device-id");
    }

    @Test
    void rejectsMismatchedTokenInsteadOfTreatingPhoneAsLoggedOut() {
        KfcUserService users = mock(KfcUserService.class);
        KfcSessionCipher cipher = mock(KfcSessionCipher.class);
        when(users.findByBrandAndPhone("KFC", "13800000000")).thenReturn(user());
        when(cipher.decrypt("encrypted-token"))
                .thenReturn("different-token".getBytes(StandardCharsets.UTF_8));
        KfcRequestContextService service = new KfcRequestContextService(users,
                mock(KfcPhoneDeviceService.class), mock(KfcInstallationService.class), cipher);

        assertThatThrownBy(() -> service.load("13800000000"))
                .isInstanceOf(KfcLoginException.class)
                .extracting("code").isEqualTo("LOGIN_STATE_CORRUPTED");
    }

    private KfcUser user() {
        KfcUser user = new KfcUser();
        user.setId(7L);
        user.setAppUserId(11L);
        user.setBrand("KFC");
        user.setPhonePlain("13800000000");
        user.setTokenPlain("upstream-token");
        user.setTokenCiphertext("encrypted-token");
        return user;
    }
}
