package com.wanghui.kfc.db.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.wanghui.kfc.db.entity.KfcUser;

/** KFC 用户映射的持久化入口。 */
public interface KfcUserService extends IService<KfcUser> {
    /**
     * 根据品牌及上游用户编码查找本地身份映射。
     * @param brand 上游品牌标识
     * @param userCode 上游用户编码
     * @return 对应用户映射，不存在时为空
     */
    KfcUser findByBrandAndUserCode(String brand, String userCode);

    /**
     * 根据品牌及登录手机号查找本地身份映射。
     *
     * @param brand 上游品牌标识
     * @param phonePlain 授权手机号明文
     * @return 对应用户映射，不存在时为空
     * @throws IllegalStateException 同一品牌和手机号存在多条映射时
     */
    KfcUser findByBrandAndPhone(String brand, String phonePlain);
}
