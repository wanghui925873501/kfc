package com.wanghui.kfc.db.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.mapper.KfcUserMapper;
import com.wanghui.kfc.db.service.KfcUserService;
import org.springframework.stereotype.Service;

/** 使用 MyBatis-Plus 持久化 KFC 用户映射。 */
@Service
public class KfcUserServiceImpl extends ServiceImpl<KfcUserMapper, KfcUser> implements KfcUserService {
    /** 供 Spring 创建用户映射服务。 */
    public KfcUserServiceImpl() { }

    /**
     * 按品牌和上游用户编码读取本地身份映射。
     * @param brand 上游品牌标识
     * @param userCode 上游用户编码
     * @return 对应用户映射，不存在时为空
     */
    @Override
    public KfcUser findByBrandAndUserCode(String brand, String userCode) {
        return lambdaQuery().eq(KfcUser::getBrand, brand).eq(KfcUser::getUserCode, userCode).one();
    }
}
