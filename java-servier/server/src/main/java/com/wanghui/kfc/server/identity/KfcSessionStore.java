package com.wanghui.kfc.server.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 以随机会话标识保存加密的本地用户引用，并使用 Redis TTL 自动过期。 */
@Service
public class KfcSessionStore {
    /** Redis 中隔离 KFC 会话的键前缀。 */
    private static final String KEY_PREFIX = "kfc:login:session:";
    /** Redis 字符串操作入口。 */
    private final StringRedisTemplate redis;
    /** 会话内容加密器。 */
    private final KfcSessionCipher cipher;
    /** 会话 JSON 编解码器。 */
    private final ObjectMapper mapper;
    /** 本地会话有效期配置。 */
    private final KfcSessionProperties properties;
    /** 生成不可猜测会话标识的安全随机源。 */
    private final SecureRandom random = new SecureRandom();

    /**
     * 创建加密会话存储。
     * @param redis Redis 字符串操作入口
     * @param cipher 会话内容加密器
     * @param mapper JSON 编解码器
     * @param properties 本地会话有效期
     */
    public KfcSessionStore(StringRedisTemplate redis, KfcSessionCipher cipher,
                           ObjectMapper mapper, KfcSessionProperties properties) {
        this.redis = redis;
        this.cipher = cipher;
        this.mapper = mapper;
        this.properties = properties;
    }

    /**
     * 签发只代表本后端会话的随机标识，上游 token 从数据库读取。
     * @param context 已验证的用户和安装引用
     * @return 交给客户端保存的本地会话标识
     * @throws IllegalStateException 有效期、密钥或序列化配置不可用时
     */
    public String issue(KfcSessionContext context) {
        if (context == null || context.getAppUserId() == null || context.getKfcUserId() == null
                || context.getInstallationDbId() == null) {
            throw new IllegalArgumentException("Complete KFC session context is required");
        }
        if (properties.getTtl() == null || properties.getTtl().isNegative()
                || properties.getTtl().isZero()) {
            throw new IllegalStateException("KFC session TTL must be positive");
        }
        try {
            byte[] idBytes = new byte[32];
            random.nextBytes(idBytes);
            String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes);
            String encrypted = cipher.encrypt(mapper.writeValueAsBytes(context));
            redis.opsForValue().set(KEY_PREFIX + sessionId, encrypted, properties.getTtl());
            return sessionId;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("KFC session could not be encoded", e);
        }
    }

    /**
     * 从 Redis 读取并验证一个本地登录会话。
     * @param sessionId 客户端持有的随机会话标识
     * @return 有效会话；不存在、过期或密文损坏时为空
     */
    public Optional<KfcSessionContext> resolve(String sessionId) {
        if (!validId(sessionId)) return Optional.empty();
        String key = KEY_PREFIX + sessionId;
        String encrypted = redis.opsForValue().get(key);
        if (encrypted == null) return Optional.empty();
        try {
            KfcSessionContext context = mapper.readValue(cipher.decrypt(encrypted), KfcSessionContext.class);
            if (context == null || context.getAppUserId() == null || context.getKfcUserId() == null
                    || context.getInstallationDbId() == null) {
                redis.delete(key);
                return Optional.empty();
            }
            return Optional.of(context);
        } catch (IOException | IllegalStateException e) {
            redis.delete(key);
            return Optional.empty();
        }
    }

    /**
     * 主动撤销一个本地登录会话。
     * @param sessionId 要撤销的随机会话标识
     */
    public void revoke(String sessionId) {
        if (validId(sessionId)) redis.delete(KEY_PREFIX + sessionId);
    }

    private boolean validId(String sessionId) {
        return sessionId != null && sessionId.matches("[A-Za-z0-9_-]{43}");
    }
}
