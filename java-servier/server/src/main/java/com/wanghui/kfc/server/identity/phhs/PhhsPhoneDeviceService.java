package com.wanghui.kfc.server.identity.phhs;

import cn.hutool.crypto.SecureUtil;
import com.wanghui.kfc.db.entity.PhhsInstallation;
import com.wanghui.kfc.db.entity.PhhsPhoneInstallation;
import com.wanghui.kfc.db.service.PhhsInstallationService;
import com.wanghui.kfc.db.service.PhhsPhoneInstallationService;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginCrypto;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 为每个授权手机号建立并复用独立的必胜客 Android 9 虚拟安装。 */
@Service
public class PhhsPhoneDeviceService {
    /** 必胜客安装记录持久化入口。 */
    private final PhhsInstallationService installations;
    /** 必胜客手机号与安装关联持久化入口。 */
    private final PhhsPhoneInstallationService phoneInstallations;
    /** 按必胜客登录协议加密手机号。 */
    private final PhhsLoginCrypto crypto;

    /**
     * 创建必胜客手机号设备上下文服务。
     * @param installations 安装记录持久化入口
     * @param phoneInstallations 手机号关联持久化入口
     * @param crypto 必胜客登录字段加密器
     */
    public PhhsPhoneDeviceService(PhhsInstallationService installations,
                                  PhhsPhoneInstallationService phoneInstallations,
                                  PhhsLoginCrypto crypto) {
        this.installations = installations;
        this.phoneInstallations = phoneInstallations;
        this.crypto = crypto;
    }

    /**
     * 首次使用手机号时创建独占安装，后续只复用该必胜客安装。
     * 设备 ID、TDID 和风险会话标识采用与 KFC 虚拟安装相同的生成规则，
     * 但数据保存到 PHHS 独立表中。
     *
     * @param phone 账号持有人授权使用的手机号
     * @param cityCode 客户端所选城市编码
     * @param userAgent 必胜客客户端 User-Agent
     * @param appVersion 必胜客客户端版本
     * @return 该手机号已持久化的必胜客安装关联
     */
    @Transactional
    public PhhsPhoneInstallation getOrCreate(String phone, String cityCode,
                                             String userAgent, String appVersion) {
        require(phone, 32, "phone");
        require(cityCode, 16, "cityCode");
        require(userAgent, 512, "userAgent");
        require(appVersion, 64, "appVersion");
        if (!phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("phone must be an 11-digit mobile number");
        }
        String phoneHash = SecureUtil.sha256(phone);
        String phoneCiphertext = crypto.encrypt(phone);
        PhhsPhoneInstallation existing = phoneInstallations.findByPhoneHash(phoneHash);
        if (existing != null) {
            PhhsInstallation saved = installations.findByInstallationId(existing.getInstallationId());
            if (!phone.equals(existing.getPhonePlain())
                    || !phoneCiphertext.equals(existing.getPhoneCiphertext())
                    || saved == null
                    || !"android-api28-phhs-virtual".equals(saved.getPlatform())) {
                throw new IllegalStateException("Stored PHHS phone installation is inconsistent");
            }
            if (!cityCode.equals(existing.getCityCode())
                    || !userAgent.equals(existing.getUserAgent())
                    || !appVersion.equals(saved.getAppVersion())) {
                throw new IllegalStateException("PHHS client profile changed; explicit update is required");
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String tdidSeed = UUID.randomUUID().toString();
        PhhsInstallation installation = new PhhsInstallation();
        installation.setInstallationId(UUID.randomUUID().toString());
        installation.setDeviceId(UUID.randomUUID() + Long.toString(System.currentTimeMillis()));
        installation.setTdid("3" + SecureUtil.md5(tdidSeed));
        installation.setPlatform("android-api28-phhs-virtual");
        installation.setAppVersion(appVersion);
        installation.setCreatedAt(now);
        installation.setLastSeenAt(now);
        if (!installations.save(installation)) {
            throw new IllegalStateException("PHHS installation could not be saved");
        }

        PhhsPhoneInstallation created = new PhhsPhoneInstallation();
        created.setPhoneHash(phoneHash);
        created.setPhonePlain(phone);
        created.setPhoneCiphertext(phoneCiphertext);
        created.setInstallationId(installation.getInstallationId());
        created.setTdidSeed(tdidSeed);
        created.setCityCode(cityCode);
        created.setUserAgent(userAgent);
        created.setRcsSessionId(UUID.randomUUID().toString());
        created.setRcsSessionStartedAt(now);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        if (!phoneInstallations.save(created)) {
            throw new IllegalStateException("PHHS phone installation could not be saved");
        }
        return created;
    }

    /**
     * 从 PHHS 独立安装记录构造登录请求上下文。
     * @param binding 手机号与必胜客安装的关联
     * @param userCode 上游用户编码，未登录时传空字符串
     * @return 可供 PHHS 登录接口使用的请求上下文
     */
    public PhhsLoginContext loginContext(PhhsPhoneInstallation binding, String userCode) {
        PhhsInstallation installation = installations.findByInstallationId(binding.getInstallationId());
        if (installation == null) {
            throw new IllegalStateException("PHHS phone installation is missing");
        }
        PhhsLoginContext context = new PhhsLoginContext();
        context.setDeviceId(installation.getDeviceId());
        context.setTdid(installation.getTdid());
        context.setJPushRegId(
                installation.getJpushRegId() == null ? "" : installation.getJpushRegId());
        context.setCityCode(binding.getCityCode());
        context.setChannel("app");
        context.setUserCode(userCode == null ? "" : userCode);
        context.setRcsdcid(binding.getRcsSessionId());
        context.setRcsav(installation.getAppVersion());
        context.setUserAgent(binding.getUserAgent());
        context.requireComplete();
        return context;
    }

    private static void require(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " is missing or too long");
        }
    }
}
