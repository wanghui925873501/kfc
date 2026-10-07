package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/** 成功登录后建立的 KFC 上游用户映射，按本机查看需求保存凭据明文与密文。 */
@Data
@TableName("kfc_user")
public class KfcUser {
    /** 供 MyBatis-Plus 构造用户映射。 */
    public KfcUser() { }

    /** 数据库内部主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对应的本地 {@code app_user.id}。 */
    private Long appUserId;
    /** 上游品牌标识。 */
    private String brand;
    /** 上游返回的 {@code userCode}。 */
    private String userCode;
    /** 上游会员标识。 */
    private String muid;
    /** 上游身份标识。 */
    private String suid;
    /** 上游会员状态。 */
    private Boolean isMember;
    /** 本次登录手机号明文，仅供有数据库访问权限的本机查看。 */
    @ToString.Exclude
    private String phonePlain;
    /** 与手机号明文对应、发送给上游的 DES 密文。 */
    @ToString.Exclude
    private String phoneCiphertext;
    /** 最近一次成功登录返回的上游 token 明文。 */
    @ToString.Exclude
    private String tokenPlain;
    /** 与 token 明文对应、由本机密钥生成的 AES-GCM 密文。 */
    @ToString.Exclude
    private String tokenCiphertext;
    /** 最近一次更新上游 token 的时间。 */
    private LocalDateTime tokenUpdatedAt;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录更新时间。 */
    private LocalDateTime updatedAt;
    /** 最近一次成功登录时间。 */
    private LocalDateTime lastLoginAt;
}
