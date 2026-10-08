-- 在手工执行 V1-V6 后执行一次；服务启动不会自动执行本脚本。
CREATE TABLE IF NOT EXISTS phhs_installation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '必胜客安装记录主键',
    installation_id CHAR(36) NOT NULL COMMENT '后端生成的安装标识',
    device_id VARCHAR(80) NOT NULL COMMENT '按客户端首次安装规则生成的设备标识',
    tdid VARCHAR(80) NOT NULL COMMENT 'Android 9 无硬件标识分支生成的 TalkingData 标识',
    jpush_reg_id VARCHAR(255) NULL COMMENT '推送 SDK 注册标识，尚未取得时为空',
    platform VARCHAR(64) NOT NULL COMMENT '安装运行平台',
    app_version VARCHAR(64) NOT NULL COMMENT '必胜客客户端版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '安装记录创建时间',
    last_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近确认时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phhs_installation_id (installation_id),
    UNIQUE KEY uk_phhs_installation_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='必胜客独立虚拟安装记录';

CREATE TABLE IF NOT EXISTS phhs_phone_installation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '必胜客手机号与安装关联记录主键',
    phone_hash CHAR(64) NOT NULL COMMENT '手机号 SHA-256，用于不暴露明文的唯一查询',
    phone_plain VARCHAR(32) NOT NULL COMMENT '授权测试手机号明文，仅供本机核对',
    phone_ciphertext VARCHAR(512) NOT NULL COMMENT '同一手机号按 PHHS 协议生成的 DES 密文',
    installation_id CHAR(36) NOT NULL COMMENT '独占的 phhs_installation.installation_id',
    tdid_seed CHAR(36) NOT NULL COMMENT 'Android 9 无硬件标识分支的随机 UUID 种子',
    city_code VARCHAR(16) NOT NULL COMMENT '客户端档案选用的城市编码',
    user_agent VARCHAR(512) NOT NULL COMMENT '必胜客客户端 User-Agent',
    rcs_session_id CHAR(36) NOT NULL COMMENT '当前模拟风控 SDK 会话 UUID',
    rcs_session_started_at DATETIME(3) NOT NULL COMMENT '当前风险会话建立时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联记录创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '关联记录最近更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phhs_phone_installation_phone_hash (phone_hash),
    UNIQUE KEY uk_phhs_phone_installation_installation_id (installation_id),
    CONSTRAINT fk_phhs_phone_installation_installation FOREIGN KEY (installation_id)
        REFERENCES phhs_installation (installation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='必胜客授权手机号与独占安装及风险会话的关联';
