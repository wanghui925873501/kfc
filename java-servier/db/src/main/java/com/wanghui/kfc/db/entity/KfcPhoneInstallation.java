package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/** 授权手机号与独占安装、客户端档案和当前风控会话的持久化关联。 */
@Data
@TableName("kfc_phone_installation")
public class KfcPhoneInstallation {
    /** 供 MyBatis-Plus 创建关联实体。 */
    public KfcPhoneInstallation() { }

    /** 数据库内部主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 手机号 SHA-256，用于唯一查询。 */
    @ToString.Exclude
    private String phoneHash;
    /** 授权手机号明文，仅供本机数据库核对。 */
    @ToString.Exclude
    private String phonePlain;
    /** 与明文对应的登录协议 DES 密文。 */
    @ToString.Exclude
    private String phoneCiphertext;
    /** 该手机号独占的安装标识。 */
    @ToString.Exclude
    private String installationId;
    /** Android 9 无硬件标识分支的随机 UUID 种子。 */
    @ToString.Exclude
    private String tdidSeed;
    /** 用户选用的城市编码。 */
    private String cityCode;
    /** 该客户端档案的 User-Agent。 */
    private String userAgent;
    /** 当前风控 SDK 会话的模拟 UUID。 */
    @ToString.Exclude
    private String rcsSessionId;
    /** 当前风控会话建立时间。 */
    private LocalDateTime rcsSessionStartedAt;
    /** 关联创建时间。 */
    private LocalDateTime createdAt;
    /** 关联最近更新时间。 */
    private LocalDateTime updatedAt;
}
