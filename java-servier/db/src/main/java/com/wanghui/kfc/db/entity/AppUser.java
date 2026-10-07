package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 本地应用用户记录，不保存第三方明文凭据。 */
@TableName("app_user")
public class AppUser {
    /** 本地用户表自增主键，不对应肯德基上游用户编号。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 在本地应用展示的用户昵称。 */
    private String nickname;
    /** 手机号的哈希值；不在本表存储手机号明文。 */
    private String phoneHash;
    /** 本地用户记录首次创建的时间。 */
    private LocalDateTime createdAt;
    /** 本地用户记录最近一次更新的时间。 */
    private LocalDateTime updatedAt;

    /** 供 MyBatis-Plus 构造实体。 */
    public AppUser() { }

    /**
     * 读取本地用户主键。
     * @return 本地用户主键
     */
    public Long getId() { return id; }
    /**
     * 设置本地用户主键。
     * @param id 本地用户主键
     */
    public void setId(Long id) { this.id = id; }
    /**
     * 读取用户昵称。
     * @return 用户昵称
     */
    public String getNickname() { return nickname; }
    /**
     * 设置用户昵称。
     * @param nickname 用户昵称
     */
    public void setNickname(String nickname) { this.nickname = nickname; }
    /**
     * 读取手机号哈希值。
     * @return 手机号的哈希值，可能为空
     */
    public String getPhoneHash() { return phoneHash; }
    /**
     * 设置手机号哈希值。
     * @param phoneHash 手机号的哈希值，不传明文
     */
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }
    /**
     * 读取记录创建时间。
     * @return 记录创建时间
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * 设置记录创建时间。
     * @param createdAt 记录创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /**
     * 读取记录最近更新时间。
     * @return 记录最近更新时间
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /**
     * 设置记录最近更新时间。
     * @param updatedAt 记录最近更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
