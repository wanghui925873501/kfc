package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.PhhsPhoneInstallation;
import com.wanghui.kfc.db.mapper.PhhsPhoneInstallationMapper;
import com.wanghui.kfc.db.service.PhhsPhoneInstallationService;
import org.springframework.stereotype.Service;

/** 用 MyBatis-Plus 保存必胜客手机号与安装的唯一关联。 */
@Service
public class PhhsPhoneInstallationServiceImpl
        extends ServiceImpl<PhhsPhoneInstallationMapper, PhhsPhoneInstallation>
        implements PhhsPhoneInstallationService {
    /** 供 Spring 创建持久化服务。 */
    public PhhsPhoneInstallationServiceImpl() { }

    /**
     * 按手机号 SHA-256 读取已有记录。
     * @param phoneHash 手机号 SHA-256
     * @return 已有记录，不存在时为空
     */
    @Override
    public PhhsPhoneInstallation findByPhoneHash(String phoneHash) {
        return lambdaQuery().eq(PhhsPhoneInstallation::getPhoneHash, phoneHash).one();
    }
}
