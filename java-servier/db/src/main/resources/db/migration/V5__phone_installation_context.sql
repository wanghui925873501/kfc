-- 在手工执行 V1、V2、V3、V4 后执行一次；服务启动不会自动执行本脚本。
CREATE TABLE IF NOT EXISTS kfc_phone_installation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '手机号与安装关联记录主键',
    phone_hash CHAR(64) NOT NULL COMMENT '手机号 SHA-256，用于不暴露明文的唯一查询',
    phone_plain VARCHAR(32) NOT NULL COMMENT '授权测试手机号明文，仅供有权限的本机数据库用户查看',
    phone_ciphertext VARCHAR(512) NOT NULL COMMENT '同一手机号按登录协议生成的 DES 密文',
    installation_id CHAR(36) NOT NULL COMMENT '该手机号独占的 kfc_installation.installation_id',
    tdid_seed CHAR(36) NOT NULL COMMENT 'Android 9 无硬件标识回退分支的随机 UUID 种子',
    city_code VARCHAR(16) NOT NULL COMMENT '该客户端档案选用的城市编码，可由用户显式调整',
    user_agent VARCHAR(512) NOT NULL COMMENT '该客户端档案的 User-Agent，可随客户端版本显式调整',
    rcs_session_id CHAR(36) NOT NULL COMMENT '当前模拟风控 SDK 会话 UUID，不等同稳定设备标识',
    rcs_session_started_at DATETIME(3) NOT NULL COMMENT '当前风控会话建立时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联记录创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '关联记录最近更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kfc_phone_installation_phone_hash (phone_hash),
    UNIQUE KEY uk_kfc_phone_installation_installation_id (installation_id),
    CONSTRAINT fk_kfc_phone_installation_installation FOREIGN KEY (installation_id)
        REFERENCES kfc_installation (installation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='授权手机号与独占安装、客户端档案和当前风控会话的持久化关联';
