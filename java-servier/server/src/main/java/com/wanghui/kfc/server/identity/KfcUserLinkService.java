package com.wanghui.kfc.server.identity;

import cn.hutool.core.util.StrUtil;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.AppUserService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在数据库事务中把一次已成功登录的上游身份关联到本地用户。 */
@Service
public class KfcUserLinkService {
    /** 本地账号查询入口。 */
    private final AppUserService appUsers;
    /** KFC 用户映射持久化入口。 */
    private final KfcUserService kfcUsers;
    /** 生成与上游请求一致的手机号密文。 */
    private final AppLoginCrypto loginCrypto;
    /** 加密本地持久化 token 的密钥入口。 */
    private final KfcSessionCipher tokenCipher;

    /**
     * 创建用户身份关联服务。
     * @param appUsers 本地账号查询入口
     * @param kfcUsers KFC 用户映射持久化入口
     * @param loginCrypto 上游手机号加密器
     * @param tokenCipher 本地 token 加密器
     */
    public KfcUserLinkService(AppUserService appUsers, KfcUserService kfcUsers,
                              AppLoginCrypto loginCrypto, KfcSessionCipher tokenCipher) {
        this.appUsers = appUsers;
        this.kfcUsers = kfcUsers;
        this.loginCrypto = loginCrypto;
        this.tokenCipher = tokenCipher;
    }

    /**
     * 保存已验证登录结果；同一上游账号不能悄悄改绑其他本地账号。
     * @param appUserId 本地账号主键
     * @param phone 本次成功登录使用的授权手机号明文
     * @param data 已成功登录的主品牌用户数据
     * @return 已持久化的 KFC 用户映射
     * @throws IllegalArgumentException 本地账号或上游用户编码不存在时
     * @throws IllegalStateException 上游账号已关联其他本地账号时
     */
    @Transactional
    public KfcUser bind(Long appUserId, String phone, LoginBySmsCodeVo.MainBrandData data) {
        if (appUserId == null || appUsers.getById(appUserId) == null) {
            throw new IllegalArgumentException("Local user does not exist");
        }
        if (StrUtil.isBlank(phone) || phone.length() > 32) {
            throw new IllegalArgumentException("Authorized phone is missing or too long");
        }
        if (data == null || StrUtil.isBlank(data.getUserCode())) {
            throw new IllegalArgumentException("Upstream user code is missing");
        }
        String token = data.getToken();
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("Upstream login token is missing");
        }
        KfcUser user = kfcUsers.findByBrandAndUserCode("KFC", data.getUserCode());
        if (user == null) {
            user = new KfcUser();
            user.setAppUserId(appUserId);
            user.setBrand("KFC");
            user.setUserCode(data.getUserCode());
        } else if (!appUserId.equals(user.getAppUserId())) {
            throw new IllegalStateException("Upstream account belongs to another local user");
        }
        user.setMuid(data.getMuid());
        user.setSuid(data.getSuid());
        user.setIsMember(data.getIsMember());
        user.setPhonePlain(phone);
        user.setPhoneCiphertext(loginCrypto.encrypt(phone));
        user.setTokenPlain(token);
        user.setTokenCiphertext(tokenCipher.encrypt(token.getBytes(StandardCharsets.UTF_8)));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setLastLoginAt(now);
        user.setTokenUpdatedAt(now);
        boolean stored;
        if (user.getId() == null) {
            stored = kfcUsers.save(user);
        } else {
            stored = kfcUsers.updateById(user);
        }
        if (!stored || user.getId() == null) {
            throw new IllegalStateException("KFC user mapping could not be saved");
        }
        return user;
    }
}
