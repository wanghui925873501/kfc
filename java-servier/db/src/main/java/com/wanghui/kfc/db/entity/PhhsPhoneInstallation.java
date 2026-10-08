package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/** 必胜客授权手机号与独占安装、客户端档案和风险会话的关联。 */
@Data
@TableName("phhs_phone_installation")
public class PhhsPhoneInstallation {
    /** 供 MyBatis-Plus 构造关联实体。 */
    public PhhsPhoneInstallation() { }

    /** 数据库内部主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 手机号 SHA-256，用于唯一查询。 */
    @ToString.Exclude
    private String phoneHash;
    /** 授权手机号明文，仅供本机数据库核对。 */
    @ToString.Exclude
    private String phonePlain;
    /** 与明文对应的 PHHS 登录协议 DES 密文。 */
    @ToString.Exclude
    private String phoneCiphertext;
    /** 该手机号独占的 PHHS 安装标识。 */
    @ToString.Exclude
    private String installationId;
    /** Android 9 无硬件标识分支的随机 UUID 种子。 */
    @ToString.Exclude
    private String tdidSeed;
    /** 用户选用的城市编码。 */
    private String cityCode;
    /** 必胜客客户端 User-Agent。 */
    private String userAgent;
    /** 当前模拟风控 SDK 会话 UUID。 */
    @ToString.Exclude
    private String rcsSessionId;
    /** 当前风险会话建立时间。 */
    private LocalDateTime rcsSessionStartedAt;
    /** 关联创建时间。 */
    private LocalDateTime createdAt;
    /** 关联最近更新时间。 */
    private LocalDateTime updatedAt;
}
