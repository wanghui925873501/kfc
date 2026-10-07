package com.wanghui.kfc.server.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class KfcSessionStoreTest {
    @Test
    void encryptsTokenWithFreshNonceAndRejectsTampering() {
        KfcSessionCipher cipher = new KfcSessionCipher(properties());
        byte[] plainText = "sample-upstream-token".getBytes(StandardCharsets.UTF_8);
        String first = cipher.encrypt(plainText);
        String second = cipher.encrypt(plainText);
        assertThat(first).isNotEqualTo(second).doesNotContain("sample-upstream-token");
        assertThat(cipher.decrypt(first)).isEqualTo(plainText);

        byte[] tampered = Base64.getUrlDecoder().decode(first);
        tampered[tampered.length - 1] ^= 1;
        String changed = Base64.getUrlEncoder().withoutPadding().encodeToString(tampered);
        assertThatThrownBy(() -> cipher.decrypt(changed)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void storesOnlyEncryptedIdentityReferencesInRedisAndExpiresSession() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        KfcSessionStore store = new KfcSessionStore(redis, new KfcSessionCipher(properties()),
                new ObjectMapper(), properties());
        KfcSessionContext context = new KfcSessionContext(1L, 2L, 3L);

        String sessionId = store.issue(context);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ciphertext = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), ciphertext.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofHours(8)));
        assertThat(sessionId).hasSize(43);
        assertThat(key.getValue()).endsWith(sessionId);
        assertThat(ciphertext.getValue()).doesNotContain("appUserId", "kfcUserId");

        when(values.get(anyString())).thenReturn(ciphertext.getValue());
        assertThat(store.resolve(sessionId)).contains(context);
        store.revoke(sessionId);
        verify(redis).delete(key.getValue());
    }

    private KfcSessionProperties properties() {
        KfcSessionProperties properties = new KfcSessionProperties();
        byte[] fakeKey = new byte[32];
        java.util.Arrays.fill(fakeKey, (byte) 7);
        properties.setKeyBase64(Base64.getEncoder().encodeToString(fakeKey));
        return properties;
    }
}
