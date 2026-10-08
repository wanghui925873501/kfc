package com.wanghui.kfc.db.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wanghui.kfc.db.entity.PhhsPhoneInstallation;

/** 必胜客手机号与独占安装关联的持久化入口。 */
public interface PhhsPhoneInstallationService extends IService<PhhsPhoneInstallation> {
    /**
     * 按手机号哈希查询已有安装关联。
     * @param phoneHash 手机号 SHA-256
     * @return 已有记录，不存在时为空
     */
    PhhsPhoneInstallation findByPhoneHash(String phoneHash);
}
