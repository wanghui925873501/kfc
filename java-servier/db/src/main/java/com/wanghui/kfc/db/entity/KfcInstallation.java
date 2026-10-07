package com.wanghui.kfc.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/** 一次客户端安装的设备上下文；手动 APK 联调记录使用 {@code android-apk-test} 平台标记。 */
@Data
@TableName("kfc_installation")
public class KfcInstallation {
    /** 供 MyBatis-Plus 构造安装记录。 */
    public KfcInstallation() { }

    /** 数据库内部主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 首次注册时由后端生成的安装标识。 */
    private String installationId;
    /** 当前应用的设备标识；仅 {@code android-apk-test} 测试记录可取自本人授权的 APK 抓包。 */
    @ToString.Exclude
    private String deviceId;
    /** 仅在实际 SDK 返回后记录的 TalkingData 标识。 */
    @ToString.Exclude
    private String tdid;
    /** 仅在推送 SDK 注册后记录的推送标识。 */
    @ToString.Exclude
    private String jpushRegId;
    /** 当前应用运行平台。 */
    private String platform;
    /** 当前应用版本；升级时可以更新。 */
    private String appVersion;
    /** 安装记录创建时间。 */
    private LocalDateTime createdAt;
    /** 最近一次确认安装上下文的时间。 */
    private LocalDateTime lastSeenAt;
}
