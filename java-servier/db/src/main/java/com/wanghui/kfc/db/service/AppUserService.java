package com.wanghui.kfc.db.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wanghui.kfc.db.entity.AppUser;

/** 本地用户的持久化服务接口。 */
public interface AppUserService extends IService<AppUser> {
    /**
     * 根据手机号 SHA-256 查找本地应用用户。
     *
     * @param phoneHash 手机号 SHA-256
     * @return 对应本地用户，不存在时为空
     */
    AppUser findByPhoneHash(String phoneHash);
}
