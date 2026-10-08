package com.wanghui.kfc.server.identity.phhs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.crypto.SecureUtil;
import com.wanghui.kfc.db.entity.PhhsInstallation;
import com.wanghui.kfc.db.entity.PhhsPhoneInstallation;
import com.wanghui.kfc.db.service.PhhsInstallationService;
import com.wanghui.kfc.db.service.PhhsPhoneInstallationService;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginCrypto;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PhhsPhoneDeviceServiceTest {
    @Test
    void createsAndReusesBrandIsolatedInstallationWithKfcGenerationRules() {
        Map<String, PhhsInstallation> installationsById = new HashMap<>();
        Map<String, PhhsPhoneInstallation> bindingsByHash = new HashMap<>();
        AtomicLong ids = new AtomicLong();
        PhhsInstallationService installations = mock(PhhsInstallationService.class);
        PhhsPhoneInstallationService bindings = mock(PhhsPhoneInstallationService.class);
        PhhsLoginCrypto crypto = mock(PhhsLoginCrypto.class);
        when(crypto.encrypt(any())).thenAnswer(call -> "cipher:" + call.getArgument(0));
        when(installations.save(any())).thenAnswer(call -> {
            PhhsInstallation value = call.getArgument(0);
            value.setId(ids.incrementAndGet());
            installationsById.put(value.getInstallationId(), value);
            return true;
        });
        when(bindings.save(any())).thenAnswer(call -> {
            PhhsPhoneInstallation value = call.getArgument(0);
            value.setId(ids.incrementAndGet());
            bindingsByHash.put(value.getPhoneHash(), value);
            return true;
        });
        when(bindings.findByPhoneHash(any()))
                .thenAnswer(call -> bindingsByHash.get(call.getArgument(0)));
        when(installations.findByInstallationId(any()))
                .thenAnswer(call -> installationsById.get(call.getArgument(0)));
        PhhsPhoneDeviceService service = new PhhsPhoneDeviceService(
                installations, bindings, crypto);

        PhhsPhoneInstallation first = service.getOrCreate(
                "13900000001", "430800", "phhs-client", "6.59.1");
        PhhsPhoneInstallation second = service.getOrCreate(
                "13900000002", "430800", "phhs-client", "6.59.1");
        PhhsPhoneInstallation again = service.getOrCreate(
                "13900000001", "430800", "phhs-client", "6.59.1");

        assertThat(again).isSameAs(first);
        assertThat(first.getInstallationId()).isNotEqualTo(second.getInstallationId());
        assertThat(first.getRcsSessionId()).isNotEqualTo(second.getRcsSessionId());
        assertThat(first.getPhoneHash()).isEqualTo(SecureUtil.sha256("13900000001"));
        assertThat(first.getPhoneCiphertext()).isEqualTo("cipher:13900000001");
        PhhsInstallation firstInstallation = installationsById.get(first.getInstallationId());
        PhhsInstallation secondInstallation = installationsById.get(second.getInstallationId());
        assertThat(firstInstallation.getDeviceId()).matches("[0-9a-f-]{36}[0-9]{13}");
        assertThat(firstInstallation.getDeviceId()).isNotEqualTo(secondInstallation.getDeviceId());
        assertThat(firstInstallation.getTdid())
                .isEqualTo("3" + SecureUtil.md5(first.getTdidSeed()));
        assertThat(firstInstallation.getTdid()).isNotEqualTo(secondInstallation.getTdid());
        assertThat(service.loginContext(first, "").getDeviceId())
                .isEqualTo(firstInstallation.getDeviceId());
        assertThat(service.loginContext(first, "").getRcsdcid())
                .isEqualTo(first.getRcsSessionId());
        assertThatThrownBy(() -> service.getOrCreate(
                "13900000001", "310000", "phhs-client", "6.59.1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
