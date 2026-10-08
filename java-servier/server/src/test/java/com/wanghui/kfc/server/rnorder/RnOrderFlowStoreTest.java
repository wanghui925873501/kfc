package com.wanghui.kfc.server.rnorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import com.wanghui.kfc.server.identity.KfcSessionCipher;
import com.wanghui.kfc.server.identity.KfcSessionProperties;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RnOrderFlowStoreTest {
    @Test
    void storesEncryptedSessionAndRejectsAnotherPhone() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RnOrderFlowProperties flowProperties = new RnOrderFlowProperties();
        flowProperties.setFlowTtl(Duration.ofMinutes(15));
        RnOrderFlowStore store = new RnOrderFlowStore(redis, cipher(), new ObjectMapper(),
                flowProperties);
        RnOrderFlowState state = state("phone-hash-a");

        String flowId = store.issue(state);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> encrypted = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), encrypted.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15)));
        assertThat(flowId).hasSize(43);
        assertThat(key.getValue()).endsWith(flowId);
        assertThat(encrypted.getValue()).doesNotContain("session-secret", "cookie-secret");

        when(values.get(anyString())).thenReturn(encrypted.getValue());
        assertThat(store.resolve(flowId, "phone-hash-a")).isPresent();
        assertThat(store.resolve(flowId, "phone-hash-b")).isEmpty();
    }

    private RnOrderFlowState state(String phoneHash) {
        RnOrderSession session = new RnOrderSession();
        session.setSessionId("session-secret");
        session.setRouteCell("route-secret");
        LinkedHashMap<String, String> cookies = new LinkedHashMap<>();
        cookies.put("route-cell", "cookie-secret");
        cookies.put("sessionIdCookie", "cookie-session");
        cookies.put("sessionIdCookie.sig", "cookie-signature");
        session.setCookies(cookies);
        RnOrderFlowState state = new RnOrderFlowState();
        state.setPhoneHash(phoneHash);
        state.setSession(session);
        return state;
    }

    private KfcSessionCipher cipher() {
        KfcSessionProperties properties = new KfcSessionProperties();
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 9);
        properties.setKeyBase64(Base64.getEncoder().encodeToString(key));
        return new KfcSessionCipher(properties);
    }
}
