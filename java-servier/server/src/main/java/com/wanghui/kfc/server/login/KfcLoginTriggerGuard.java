package com.wanghui.kfc.server.login;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 使用 Redis 对同一手机号的登录外部请求进行跨进程互斥和冷却。 */
@Service
public class KfcLoginTriggerGuard {
    /** 登录互斥 Redis 键前缀。 */
    private static final String GATE_PREFIX = "kfc:login:gate:";
    /** 短信发送冷却 Redis 键前缀。 */
    private static final String SMS_PREFIX = "kfc:login:sms:";
    /** 验证码登录冷却 Redis 键前缀。 */
    private static final String LOGIN_PREFIX = "kfc:login:verify:";
    /** 仅在锁值仍属于当前调用时删除互斥锁。 */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    /** 记录不含手机号和锁值的 Redis 清理异常。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(KfcLoginTriggerGuard.class);
    /** Redis 字符串操作入口。 */
    private final StringRedisTemplate redis;
    /** 互斥与冷却时长配置。 */
    private final KfcLoginGuardProperties properties;

    /**
     * 创建登录触发保护器。
     *
     * @param redis Redis 字符串操作入口
     * @param properties 互斥与冷却时长配置
     */
    public KfcLoginTriggerGuard(StringRedisTemplate redis, KfcLoginGuardProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /**
     * 在同一手机号的分布式互斥区内执行业务，并在锁内重新检查本地登录态。
     *
     * @param phoneHash 手机号 SHA-256，不使用手机号明文作为 Redis 键
     * @param action 互斥区内执行的业务
     * @param <T> 业务返回类型
     * @return 业务返回值
     * @throws KfcLoginException 同一手机号已有请求执行中或 Redis 不可用时
     */
    public <T> T withPhoneGate(String phoneHash, Supplier<T> action) {
        String key = GATE_PREFIX + phoneHash;
        String owner = UUID.randomUUID().toString();
        boolean acquired = setIfAbsent(key, owner, properties.getMutexTtl());
        if (!acquired) {
            throw new KfcLoginException("LOGIN_BUSY", "该手机号已有登录请求处理中，请稍后再试",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            return action.get();
        } finally {
            release(key, owner);
        }
    }

    /**
     * 为一次短信发送占用冷却窗口；窗口内不会再次调用 KFC 发码接口。
     *
     * @param phoneHash 手机号 SHA-256
     * @throws KfcLoginException 冷却期未结束或 Redis 不可用时
     */
    public void claimSmsCooldown(String phoneHash) {
        if (!setIfAbsent(SMS_PREFIX + phoneHash, "1", properties.getSmsCooldown())) {
            throw new KfcLoginException("SMS_SEND_TOO_FREQUENT", "验证码发送过于频繁，请稍后再试",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 为一次验证码登录占用短期窗口，阻止并发或双击重复提交。
     *
     * @param phoneHash 手机号 SHA-256
     * @throws KfcLoginException 冷却期未结束或 Redis 不可用时
     */
    public void claimLoginCooldown(String phoneHash) {
        if (!setIfAbsent(LOGIN_PREFIX + phoneHash, "1", properties.getLoginCooldown())) {
            throw new KfcLoginException("LOGIN_TOO_FREQUENT", "验证码登录请求过于频繁，请稍后再试",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private boolean setIfAbsent(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("KFC login guard duration must be positive");
        }
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
        } catch (DataAccessException e) {
            throw new KfcLoginException("LOGIN_GUARD_UNAVAILABLE", "登录触发保护暂不可用",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void release(String key, String owner) {
        try {
            redis.execute(RELEASE_SCRIPT, List.of(key), owner);
        } catch (DataAccessException e) {
            // 锁仍会由 TTL 自动清理；不在外部请求完成后用清理故障覆盖真实业务结果。
            LOGGER.warn("KFC login gate release failed; the lock will expire automatically");
        }
    }
}
