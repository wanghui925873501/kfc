package com.wanghui.kfc.db.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wanghui.kfc.db.entity.KfcInstallation;

/** 应用安装记录的持久化入口。 */
public interface KfcInstallationService extends IService<KfcInstallation> {
    /**
     * 根据一次应用安装的公开标识查找持久化记录。
     * @param installationId 安装标识
     * @return 对应安装记录，不存在时为空
     */
    KfcInstallation findByInstallationId(String installationId);
}
