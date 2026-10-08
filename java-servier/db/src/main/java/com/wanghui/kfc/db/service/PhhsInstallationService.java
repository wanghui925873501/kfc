package com.wanghui.kfc.db.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wanghui.kfc.db.entity.PhhsInstallation;

/** 必胜客虚拟安装的持久化入口。 */
public interface PhhsInstallationService extends IService<PhhsInstallation> {
    /**
     * 按业务安装标识查询安装记录。
     * @param installationId 安装标识
     * @return 已有记录，不存在时为空
     */
    PhhsInstallation findByInstallationId(String installationId);
}
