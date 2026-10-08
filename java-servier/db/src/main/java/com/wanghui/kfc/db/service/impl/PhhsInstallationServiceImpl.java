package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.PhhsInstallation;
import com.wanghui.kfc.db.mapper.PhhsInstallationMapper;
import com.wanghui.kfc.db.service.PhhsInstallationService;
import org.springframework.stereotype.Service;

/** 用 MyBatis-Plus 保存必胜客虚拟安装。 */
@Service
public class PhhsInstallationServiceImpl
        extends ServiceImpl<PhhsInstallationMapper, PhhsInstallation>
        implements PhhsInstallationService {
    /** 供 Spring 创建持久化服务。 */
    public PhhsInstallationServiceImpl() { }

    /**
     * 按业务安装标识读取安装。
     * @param installationId 安装标识
     * @return 已有记录，不存在时为空
     */
    @Override
    public PhhsInstallation findByInstallationId(String installationId) {
        return lambdaQuery().eq(PhhsInstallation::getInstallationId, installationId).one();
    }
}
