package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.mapper.KfcInstallationMapper;
import com.wanghui.kfc.db.service.KfcInstallationService;
import org.springframework.stereotype.Service;

/** 使用 MyBatis-Plus 持久化应用安装记录。 */
@Service
public class KfcInstallationServiceImpl extends ServiceImpl<KfcInstallationMapper, KfcInstallation>
        implements KfcInstallationService {
    /** 供 Spring 创建安装记录服务。 */
    public KfcInstallationServiceImpl() { }

    /**
     * 按一次安装的标识读取设备上下文。
     * @param installationId 安装标识
     * @return 对应安装记录，不存在时为空
     */
    @Override
    public KfcInstallation findByInstallationId(String installationId) {
        return lambdaQuery().eq(KfcInstallation::getInstallationId, installationId).one();
    }
}
