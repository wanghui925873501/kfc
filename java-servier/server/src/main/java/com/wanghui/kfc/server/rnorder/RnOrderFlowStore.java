package com.wanghui.kfc.server.rnorder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.server.identity.KfcSessionCipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 使用加密 Redis 记录保存短期 RN 点餐流程，不向前端暴露上游会话。 */
@Service
public class RnOrderFlowStore {
    /** Redis 中隔离 RN 点餐流程的键前缀。 */
    private static final String KEY_PREFIX = "kfc:rn-order:flow:";
    /** Redis 字符串操作入口。 */
    private final StringRedisTemplate redis;
    /** 流程状态 AES-GCM 加密器。 */
    private final KfcSessionCipher cipher;
    /** 流程状态 JSON 编解码器。 */
    private final ObjectMapper mapper;
    /** RN 点餐流程有效期配置。 */
    private final RnOrderFlowProperties properties;
    /** 生成不可猜测流程标识的安全随机源。 */
    private final SecureRandom random = new SecureRandom();

    /**
     * 创建加密 RN 点餐流程存储。
     *
     * @param redis Redis 字符串操作入口
     * @param cipher 流程内容加密器
     * @param mapper JSON 编解码器
     * @param properties 流程有效期配置
     */
    public RnOrderFlowStore(StringRedisTemplate redis, KfcSessionCipher cipher,
                            ObjectMapper mapper, RnOrderFlowProperties properties) {
        this.redis = redis;
        this.cipher = cipher;
        this.mapper = mapper;
        this.properties = properties;
    }

    /**
     * 为已初始化的上游会话签发一个前端可持有的随机流程标识。
     *
     * @param state 已绑定手机号摘要的完整上游会话
     * @return 不包含任何上游凭据的随机流程标识
     */
    public String issue(RnOrderFlowState state) {
        requireState(state);
        byte[] idBytes = new byte[32];
        random.nextBytes(idBytes);
        String flowId = Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes);
        save(flowId, state);
        return flowId;
    }

    /**
     * 读取并核对一个手机号所属的 RN 点餐流程。
     *
     * @param flowId 前端持有的随机流程标识
     * @param phoneHash 当前登录手机号的 SHA-256
     * @return 有效且属于当前手机号的流程；不存在、过期或损坏时为空
     */
    public Optional<RnOrderFlowState> resolve(String flowId, String phoneHash) {
        if (!validId(flowId) || phoneHash == null || phoneHash.isBlank()) return Optional.empty();
        String key = KEY_PREFIX + flowId;
        String encrypted = redis.opsForValue().get(key);
        if (encrypted == null) return Optional.empty();
        try {
            RnOrderFlowState state = mapper.readValue(cipher.decrypt(encrypted),
                    RnOrderFlowState.class);
            requireState(state);
            if (!sameHash(phoneHash, state.getPhoneHash())) return Optional.empty();
            return Optional.of(state);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            redis.delete(key);
            return Optional.empty();
        }
    }

    /**
     * 保存调用后可能更新了 Cookie 或路由单元的流程，并刷新有效期。
     *
     * @param flowId 已签发的随机流程标识
     * @param state 更新后的内部流程状态
     */
    public void save(String flowId, RnOrderFlowState state) {
        if (!validId(flowId)) throw new IllegalArgumentException("RN order flow id is invalid");
        requireState(state);
        try {
            String encrypted = cipher.encrypt(mapper.writeValueAsBytes(state));
            redis.opsForValue().set(KEY_PREFIX + flowId, encrypted, ttl());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RN order flow could not be encoded", e);
        }
    }

    /**
     * 返回前端可用于安排流程续期的有效秒数。
     *
     * @return 正数秒数
     */
    public long expiresInSeconds() {
        return ttl().toSeconds();
    }

    private Duration ttl() {
        Duration ttl = properties.getFlowTtl();
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("RN order flow TTL must be positive");
        }
        return ttl;
    }

    private void requireState(RnOrderFlowState state) {
        if (state == null || state.getPhoneHash() == null || state.getPhoneHash().isBlank()
                || state.getSession() == null) {
            throw new IllegalArgumentException("Complete RN order flow state is required");
        }
        state.getSession().requireComplete();
    }

    private boolean validId(String flowId) {
        return flowId != null && flowId.matches("[A-Za-z0-9_-]{43}");
    }

    private boolean sameHash(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
