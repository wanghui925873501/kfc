package com.wanghui.kfc.server.context;

import cn.hutool.core.util.StrUtil;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.server.identity.KfcPhoneDeviceService;
import com.wanghui.kfc.server.identity.KfcSessionCipher;
import com.wanghui.kfc.server.login.KfcLoginException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 根据手机号从本地数据库建立后续 KFC 业务调用所需的完整请求上下文。 */
@Service
public class KfcRequestContextService {
    /** 固定的 KFC 品牌标识。 */
    private static final String BRAND = "KFC";
    /** KFC 用户映射查询入口。 */
    private final KfcUserService users;
    /** 手机号独占安装上下文服务。 */
    private final KfcPhoneDeviceService phoneDevices;
    /** 安装设备数据查询入口。 */
    private final KfcInstallationService installations;
    /** 解密并核对数据库 token 密文的服务。 */
    private final KfcSessionCipher tokenCipher;

    /**
     * 创建手机号请求上下文加载服务。
     *
     * @param users KFC 用户映射查询入口
     * @param phoneDevices 手机号独占安装上下文服务
     * @param installations 安装设备数据查询入口
     * @param tokenCipher token 密文解密器
     */
    public KfcRequestContextService(KfcUserService users, KfcPhoneDeviceService phoneDevices,
                                    KfcInstallationService installations, KfcSessionCipher tokenCipher) {
        this.users = users;
        this.phoneDevices = phoneDevices;
        this.installations = installations;
        this.tokenCipher = tokenCipher;
    }

    /**
     * 查询手机号对应的已登录用户、已验证 token 和设备上下文。
     *
     * @param phone 当前请求携带的手机号
     * @return 完整已登录上下文；用户尚未登录时为空
     * @throws KfcLoginException 本地用户、token 或安装数据不一致时
     */
    public Optional<KfcRequestContext> load(String phone) {
        KfcUser user;
        try {
            user = users.findByBrandAndPhone(BRAND, phone);
        } catch (IllegalStateException e) {
            throw corrupted();
        }
        if (user == null) return Optional.empty();
        boolean plainMissing = StrUtil.isBlank(user.getTokenPlain());
        boolean cipherMissing = StrUtil.isBlank(user.getTokenCiphertext());
        if (plainMissing && cipherMissing) return Optional.empty();
        if (plainMissing || cipherMissing) throw corrupted();
        String token;
        try {
            token = new String(tokenCipher.decrypt(user.getTokenCiphertext()), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw corrupted();
        }
        if (!token.equals(user.getTokenPlain())) throw corrupted();
        KfcPhoneInstallation phoneInstallation;
        try {
            phoneInstallation = phoneDevices.findExisting(phone);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw corrupted();
        }
        if (phoneInstallation == null) throw corrupted();
        KfcInstallation installation = installations.findByInstallationId(
                phoneInstallation.getInstallationId());
        if (installation == null || installation.getId() == null) throw corrupted();
        KfcRequestContext context = new KfcRequestContext();
        context.setPhone(phone);
        context.setKfcUser(user);
        context.setUpstreamToken(token);
        context.setInstallation(installation);
        context.setPhoneInstallation(phoneInstallation);
        return Optional.of(context);
    }

    private KfcLoginException corrupted() {
        return new KfcLoginException("LOGIN_STATE_CORRUPTED", "本地登录数据不完整，已停止业务调用",
                HttpStatus.CONFLICT);
    }
}
