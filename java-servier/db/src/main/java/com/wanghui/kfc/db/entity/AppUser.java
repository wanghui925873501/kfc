package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 本地应用用户记录，不保存第三方明文凭据。 */
@Data
@TableName("app_user")
public class AppUser {
    /** 供 MyBatis-Plus 构造本地用户实体。 */
    public AppUser() { }

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
}
