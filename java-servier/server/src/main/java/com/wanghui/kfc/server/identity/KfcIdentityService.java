package com.wanghui.kfc.server.identity;

import cn.hutool.core.util.StrUtil;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 贯通未登录安装记录、登录后的用户映射和后续上游调用上下文。 */
@Service
public class KfcIdentityService {
    /** 安装记录持久化入口。 */
    private final KfcInstallationService installations;
    /** 登录成功后的上游账号绑定服务。 */
    private final KfcUserLinkService userLink;
    /** KFC 用户映射查询入口。 */
    private final KfcUserService kfcUsers;
    /** 只保存加密内容的本地会话存储。 */
    private final KfcSessionStore sessions;
    /** 从数据库密文恢复上游 token 的加密器。 */
    private final KfcSessionCipher tokenCipher;

    /**
     * 创建用户与安装上下文编排服务。
     * @param installations 安装记录持久化入口
     * @param userLink 上游账号绑定服务
     * @param kfcUsers KFC 用户映射查询入口
     * @param sessions 加密会话存储
     * @param tokenCipher 持久化 token 解密器
     */
    public KfcIdentityService(KfcInstallationService installations, KfcUserLinkService userLink,
                              KfcUserService kfcUsers, KfcSessionStore sessions,
                              KfcSessionCipher tokenCipher) {
        this.installations = installations;
        this.userLink = userLink;
        this.kfcUsers = kfcUsers;
        this.sessions = sessions;
        this.tokenCipher = tokenCipher;
    }

    /**
     * 在登录前注册一次安装；设备标识必须来自当前应用，不能从其他用户抓包复制。
     * @param deviceId 当前应用设备 API 返回的标识
     * @param platform 当前应用平台
     * @param appVersion 当前应用版本，可能为空
     * @return 已保存、可在后续请求中复用的安装记录
     * @throws IllegalArgumentException 必需字段缺失或超过数据库长度时
     */
    public KfcInstallation registerInstallation(String deviceId, String platform, String appVersion) {
        requireLength(deviceId, 128, "deviceId");
        requireLength(platform, 32, "platform");
        optionalLength(appVersion, 64, "appVersion");
        KfcInstallation installation = new KfcInstallation();
        installation.setInstallationId(UUID.randomUUID().toString());
        installation.setDeviceId(deviceId);
        installation.setPlatform(platform);
        installation.setAppVersion(appVersion);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        installation.setCreatedAt(now);
        installation.setLastSeenAt(now);
        if (!installations.save(installation) || installation.getId() == null) {
            throw new IllegalStateException("KFC installation could not be saved");
        }
        return installation;
    }

    /**
     * 更新同一次安装的当前版本与实际 SDK 标识，不更换设备标识。
     * @param installationId 注册时返回的安装标识
     * @param deviceId 当前应用设备 API 返回的标识
     * @param platform 当前应用平台
     * @param appVersion 当前应用版本，可能为空
     * @param tdid 实际 SDK 标识；传空字符串表示清除，传 null 表示不更新
     * @param jpushRegId 实际推送注册标识；传空字符串表示清除，传 null 表示不更新
     * @return 更新后的安装记录
     * @throws IllegalArgumentException 安装不存在或设备标识变化时
     */
    public KfcInstallation refreshInstallation(String installationId, String deviceId, String platform,
                                               String appVersion, String tdid, String jpushRegId) {
        requireLength(installationId, 36, "installationId");
        requireLength(deviceId, 128, "deviceId");
        requireLength(platform, 32, "platform");
        optionalLength(appVersion, 64, "appVersion");
        optionalLength(tdid, 128, "tdid");
        optionalLength(jpushRegId, 255, "jpushRegId");
        KfcInstallation installation = findInstallation(installationId);
        if (installation == null || !deviceId.equals(installation.getDeviceId())) {
            throw new IllegalArgumentException("Installation is missing or device identifier changed");
        }
        installation.setPlatform(platform);
        installation.setAppVersion(appVersion);
        if (tdid != null) installation.setTdid(tdid.isBlank() ? null : tdid);
        if (jpushRegId != null) installation.setJpushRegId(jpushRegId.isBlank() ? null : jpushRegId);
        installation.setLastSeenAt(LocalDateTime.now(ZoneOffset.UTC));
        if (!installations.updateById(installation)) {
            throw new IllegalStateException("KFC installation could not be updated");
        }
        return installation;
    }

    /**
     * 仅在短信登录成功后建立用户映射并签发本应用会话。
     * @param appUserId 已建立的本地账号主键
     * @param installationId 本次登录使用的安装标识
     * @param phone 本次登录使用的授权手机号明文
     * @param response 已验证成功的登录接口响应
     * @return 不含上游 token 的本地会话句柄
     * @throws IllegalArgumentException 安装、用户或成功登录结果缺失时
     */
    public IssuedKfcSession recordLoginSuccess(Long appUserId, String installationId, String phone,
                                                LoginBySmsCodeVo response) {
        KfcInstallation installation = findInstallation(installationId);
        if (installation == null || response == null || response.getErrCode() == null
                || response.getErrCode() != 0 || response.getData() == null
                || response.getData().getMainBrandData() == null) {
            throw new IllegalArgumentException("Verified login result or installation is missing");
        }
        LoginBySmsCodeVo.MainBrandData data = response.getData().getMainBrandData();
        if (StrUtil.isBlank(data.getToken())) {
            throw new IllegalArgumentException("Upstream login token is missing");
        }
        KfcUser user = userLink.bind(appUserId, phone, data);
        String sessionId = sessions.issue(new KfcSessionContext(appUserId, user.getId(),
                installation.getId()));
        return new IssuedKfcSession(sessionId, user.getId());
    }

    /**
     * 在后续业务请求中读取已登录用户与同一次安装的上游调用上下文。
     * @param sessionId 本应用会话标识
     * @param installationId 客户端当前安装标识
     * @return 有效的后端内部上下文；会话过期或绑定不一致时为空
     */
    public Optional<KfcUpstreamContext> resolveUpstreamContext(String sessionId, String installationId) {
        Optional<KfcSessionContext> stored = sessions.resolve(sessionId);
        if (stored.isEmpty()) return Optional.empty();
        KfcSessionContext session = stored.get();
        KfcInstallation installation = findInstallation(installationId);
        KfcUser user = kfcUsers.getById(session.getKfcUserId());
        if (installation == null || user == null || !installation.getId().equals(session.getInstallationDbId())
                || !user.getAppUserId().equals(session.getAppUserId())
                || StrUtil.isBlank(user.getTokenPlain()) || StrUtil.isBlank(user.getTokenCiphertext())) {
            sessions.revoke(sessionId);
            return Optional.empty();
        }
        String decrypted;
        try {
            decrypted = new String(tokenCipher.decrypt(user.getTokenCiphertext()), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            sessions.revoke(sessionId);
            return Optional.empty();
        }
        if (!decrypted.equals(user.getTokenPlain())) {
            sessions.revoke(sessionId);
            return Optional.empty();
        }
        return Optional.of(new KfcUpstreamContext(user.getUserCode(), decrypted,
                installation.getDeviceId(), installation.getTdid(), installation.getJpushRegId(),
                installation.getPlatform(), installation.getAppVersion()));
    }

    private KfcInstallation findInstallation(String installationId) {
        if (installationId == null || installationId.isBlank()) return null;
        return installations.findByInstallationId(installationId);
    }

    private void requireLength(String value, int max, String name) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(name + " is missing or too long");
        }
    }

    private void optionalLength(String value, int max, String name) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(name + " is too long");
        }
    }
}
