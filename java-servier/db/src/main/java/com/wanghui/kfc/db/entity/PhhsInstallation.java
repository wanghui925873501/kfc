package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/** 必胜客客户端的一次独立虚拟安装。 */
@Data
@TableName("phhs_installation")
public class PhhsInstallation {
    /** 供 MyBatis-Plus 构造安装记录。 */
    public PhhsInstallation() { }

    /** 数据库内部主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 首次注册时由后端生成的安装标识。 */
    private String installationId;
    /** 当前安装的设备标识。 */
    @ToString.Exclude
    private String deviceId;
    /** Android 9 无硬件标识分支使用的 TalkingData 标识。 */
    @ToString.Exclude
    private String tdid;
    /** 推送 SDK 注册标识，尚未注册时为空。 */
    @ToString.Exclude
    private String jpushRegId;
    /** 当前应用运行平台。 */
    private String platform;
    /** 当前必胜客客户端版本。 */
    private String appVersion;
    /** 安装记录创建时间。 */
    private LocalDateTime createdAt;
    /** 最近一次确认安装上下文的时间。 */
    private LocalDateTime lastSeenAt;
}
