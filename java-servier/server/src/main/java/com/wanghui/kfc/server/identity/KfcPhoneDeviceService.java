package com.wanghui.kfc.server.identity;

import cn.hutool.crypto.SecureUtil;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcPhoneInstallationService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 为每个授权手机号建立并复用独占的 Android 9 虚拟安装上下文。 */
@Service
public class KfcPhoneDeviceService {
    /** 安装记录持久化入口。 */
    private final KfcInstallationService installations;
    /** 手机号与安装的唯一关联持久化入口。 */
    private final KfcPhoneInstallationService phoneInstallations;
    /** 按登录协议加密手机号。 */
    private final AppLoginCrypto crypto;

    /**
     * 创建手机号设备上下文服务。
     * @param installations 安装记录持久化入口
     * @param phoneInstallations 手机号关联持久化入口
     * @param crypto 登录字段加密器
     */
    public KfcPhoneDeviceService(KfcInstallationService installations,
                                 KfcPhoneInstallationService phoneInstallations, AppLoginCrypto crypto) {
        this.installations = installations;
        this.phoneInstallations = phoneInstallations;
        this.crypto = crypto;
    }

    /**
     * 首次使用手机号时创建独占安装；后续仅复用，客户端档案变更须显式处理。
     * Android 9 无可用硬件标识时，APK 的 TalkingData 回退分支使用 {@code 3 + MD5(UUID)}。
     * 这不是风控 SDK 遥测或真实物理设备证明，不能保证上游放行。
     * @param phone 账号持有人授权使用的手机号
     * @param cityCode 客户端所选城市编码
     * @param userAgent 当前客户端档案的 User-Agent
     * @param appVersion 当前客户端版本
     * @return 该手机号已持久化的独占安装关联
     * @throws IllegalArgumentException 手机号或客户端档案无效时
     * @throws IllegalStateException 既有记录损坏或保存失败时
     */
    @Transactional
    public KfcPhoneInstallation getOrCreate(String phone, String cityCode,
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
        KfcPhoneInstallation existing = phoneInstallations.findByPhoneHash(phoneHash);
        if (existing != null) {
            KfcInstallation savedInstallation = installations.findByInstallationId(existing.getInstallationId());
            if (!phone.equals(existing.getPhonePlain())
                    || !phoneCiphertext.equals(existing.getPhoneCiphertext())
                    || savedInstallation == null
                    || !"android-api28-virtual".equals(savedInstallation.getPlatform())) {
                throw new IllegalStateException("Stored phone installation is inconsistent");
            }
            if (!cityCode.equals(existing.getCityCode()) || !userAgent.equals(existing.getUserAgent())
                    || !appVersion.equals(savedInstallation.getAppVersion())) {
                throw new IllegalStateException("Client profile changed; explicit profile update is required");
            }
            return existing;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String tdidSeed = UUID.randomUUID().toString();
        KfcInstallation installation = new KfcInstallation();
        installation.setInstallationId(UUID.randomUUID().toString());
        installation.setDeviceId(UUID.randomUUID() + Long.toString(System.currentTimeMillis()));
        installation.setTdid("3" + SecureUtil.md5(tdidSeed));
        installation.setPlatform("android-api28-virtual");
        installation.setAppVersion(appVersion);
        installation.setCreatedAt(now);
        installation.setLastSeenAt(now);
        if (!installations.save(installation)) {
            throw new IllegalStateException("Installation could not be saved");
        }
        KfcPhoneInstallation created = new KfcPhoneInstallation();
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
            throw new IllegalStateException("Phone installation could not be saved");
        }
        return created;
    }

    /**
     * 读取手机号已有的安装关联，供已登录账号复用 token 时签发本地会话。
     * 该操作不改变既有客户端档案，也不会创建记录或请求 KFC。
     *
     * @param phone 账号持有人授权使用的手机号
     * @return 已持久化的安装关联，不存在时为空
     * @throws IllegalArgumentException 手机号格式无效时
     * @throws IllegalStateException 既有关联的手机号密文、安装或平台不一致时
     */
    public KfcPhoneInstallation findExisting(String phone) {
        require(phone, 32, "phone");
        if (!phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("phone must be an 11-digit mobile number");
        }
        KfcPhoneInstallation existing = phoneInstallations.findByPhoneHash(SecureUtil.sha256(phone));
        if (existing == null) return null;
        KfcInstallation installation = installations.findByInstallationId(existing.getInstallationId());
        if (!phone.equals(existing.getPhonePlain())
                || !crypto.encrypt(phone).equals(existing.getPhoneCiphertext())
                || installation == null
                || !"android-api28-virtual".equals(installation.getPlatform())) {
            throw new IllegalStateException("Stored phone installation is inconsistent");
        }
        return existing;
    }

    /**
     * 从持久化安装与会话构造本次登录请求上下文。
     * @param binding 手机号与安装的关联
     * @param userCode 上游已确认的用户编码，未登录时传空字符串
     * @return 可供登录接口使用的请求上下文
     * @throws IllegalStateException 关联对应的安装不存在时
     */
    public AppLoginContext loginContext(KfcPhoneInstallation binding, String userCode) {
        KfcInstallation installation = installations.findByInstallationId(binding.getInstallationId());
        if (installation == null) throw new IllegalStateException("Phone installation is missing");
        AppLoginContext context = new AppLoginContext();
        context.setDeviceId(installation.getDeviceId());
        context.setTdid(installation.getTdid());
        context.setJPushRegId(installation.getJpushRegId() == null ? "" : installation.getJpushRegId());
        context.setCityCode(binding.getCityCode());
        context.setChannel("app");
        context.setUserCode(userCode == null ? "" : userCode);
        context.setRcsdcid(binding.getRcsSessionId());
        context.setRcsav(installation.getAppVersion());
        context.setUserAgent(binding.getUserAgent());
        context.requireComplete();
        return context;
    }

    /**
     * 用户显式开始新的 SDK 会话时更换会话 UUID，稳定安装标识保持不变。
     * @param binding 已保存的手机号与安装关联
     * @return 更新后的会话关联
     * @throws IllegalStateException 数据库更新失败时
     */
    @Transactional
    public KfcPhoneInstallation startNewRiskSession(KfcPhoneInstallation binding) {
        if (binding == null || binding.getId() == null) {
            throw new IllegalArgumentException("Persisted phone installation is required");
        }
        binding.setRcsSessionId(UUID.randomUUID().toString());
        binding.setRcsSessionStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (!phoneInstallations.updateById(binding)) {
            throw new IllegalStateException("Risk session could not be saved");
        }
        return binding;
    }

    private void require(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " is missing or too long");
        }
    }
}
