package com.wanghui.kfc.server.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 核验 Redis 登录锁的互斥与冷却语义，不连接真实 Redis。 */
class KfcLoginTriggerGuardTest {
    @Test
    void executesOnceWhenPhoneGateIsAcquired() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        KfcLoginTriggerGuard guard = new KfcLoginTriggerGuard(redis, new KfcLoginGuardProperties());

        assertThat(guard.withPhoneGate("phone-hash", () -> "done")).isEqualTo("done");
        verify(redis).execute(any(), any(), anyString());
    }

    @Test
    void rejectsConcurrentPhoneGateWithoutRunningAction() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        KfcLoginTriggerGuard guard = new KfcLoginTriggerGuard(redis, new KfcLoginGuardProperties());

        assertThatThrownBy(() -> guard.withPhoneGate("phone-hash", () -> "not-run"))
                .isInstanceOf(KfcLoginException.class)
                .extracting("code").isEqualTo("LOGIN_BUSY");
    }
}
