package com.wanghui.kfc.server.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.crypto.SecureUtil;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcPhoneInstallationService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class KfcPhoneDeviceServiceTest {
    @Test
    void assignsDifferentStableInstallationsAndReusesEachPhoneContext() {
        Map<String, KfcInstallation> installationsById = new HashMap<>();
        Map<String, KfcPhoneInstallation> bindingsByHash = new HashMap<>();
        AtomicLong ids = new AtomicLong();
        KfcInstallationService installations = mock(KfcInstallationService.class);
        KfcPhoneInstallationService bindings = mock(KfcPhoneInstallationService.class);
        AppLoginCrypto crypto = mock(AppLoginCrypto.class);
        when(crypto.encrypt(any())).thenAnswer(call -> "cipher:" + call.getArgument(0));
        when(installations.save(any())).thenAnswer(call -> {
            KfcInstallation value = call.getArgument(0);
            value.setId(ids.incrementAndGet());
            installationsById.put(value.getInstallationId(), value);
            return true;
        });
        when(bindings.save(any())).thenAnswer(call -> {
            KfcPhoneInstallation value = call.getArgument(0);
            value.setId(ids.incrementAndGet());
            bindingsByHash.put(value.getPhoneHash(), value);
            return true;
        });
        when(bindings.findByPhoneHash(any())).thenAnswer(call -> bindingsByHash.get(call.getArgument(0)));
        when(installations.findByInstallationId(any()))
                .thenAnswer(call -> installationsById.get(call.getArgument(0)));
        KfcPhoneDeviceService service = new KfcPhoneDeviceService(installations, bindings, crypto);

        KfcPhoneInstallation first = service.getOrCreate("13900000001", "310000", "test-client", "6.37.0");
        KfcPhoneInstallation second = service.getOrCreate("13900000002", "310000", "test-client", "6.37.0");
        KfcPhoneInstallation again = service.getOrCreate("13900000001", "310000", "test-client", "6.37.0");

        assertThat(again).isSameAs(first);
        assertThat(first.getInstallationId()).isNotEqualTo(second.getInstallationId());
        assertThat(first.getRcsSessionId()).isNotEqualTo(second.getRcsSessionId());
        assertThat(first.getPhoneHash()).isEqualTo(SecureUtil.sha256("13900000001"));
        assertThat(first.getPhoneCiphertext()).isEqualTo("cipher:13900000001");
        KfcInstallation firstInstallation = installationsById.get(first.getInstallationId());
        KfcInstallation secondInstallation = installationsById.get(second.getInstallationId());
        assertThat(firstInstallation.getDeviceId()).matches(
                "[0-9a-f-]{36}[0-9]{13}");
        assertThat(firstInstallation.getDeviceId()).isNotEqualTo(secondInstallation.getDeviceId());
        assertThat(firstInstallation.getTdid()).isEqualTo("3" + SecureUtil.md5(first.getTdidSeed()));
        assertThat(firstInstallation.getTdid()).isNotEqualTo(secondInstallation.getTdid());
        assertThat(service.loginContext(first, "").getDeviceId()).isEqualTo(firstInstallation.getDeviceId());
        assertThat(service.loginContext(first, "").getRcsdcid()).isEqualTo(first.getRcsSessionId());
        assertThat(service.findExisting("13900000001")).isSameAs(first);
        assertThatThrownBy(() -> service.getOrCreate("13900000001", "430800", "test-client", "6.37.0"))
                .isInstanceOf(IllegalStateException.class);
    }
}
