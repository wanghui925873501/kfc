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
}
