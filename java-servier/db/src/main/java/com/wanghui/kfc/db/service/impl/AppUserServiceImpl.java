package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.AppUser;
import com.wanghui.kfc.db.mapper.AppUserMapper;
import com.wanghui.kfc.db.service.AppUserService;
import org.springframework.stereotype.Service;

/** 本地用户持久化服务的 MyBatis-Plus 实现。 */
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {
    /** 供 Spring 创建用户持久化服务。 */
    public AppUserServiceImpl() { }
}
