package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.mapper.KfcPhoneInstallationMapper;
import com.wanghui.kfc.db.service.KfcPhoneInstallationService;
import org.springframework.stereotype.Service;

/** 用 MyBatis-Plus 保存手机号与安装的唯一关联。 */
@Service
public class KfcPhoneInstallationServiceImpl
        extends ServiceImpl<KfcPhoneInstallationMapper, KfcPhoneInstallation>
        implements KfcPhoneInstallationService {
    /** 供 Spring 创建持久化服务。 */
    public KfcPhoneInstallationServiceImpl() { }

    /**
     * 根据手机号 SHA-256 读取已有记录。
     * @param phoneHash 手机号 SHA-256
     * @return 已有记录，不存在时为空
     */
    @Override
    public KfcPhoneInstallation findByPhoneHash(String phoneHash) {
        return lambdaQuery().eq(KfcPhoneInstallation::getPhoneHash, phoneHash).one();
    }
}
