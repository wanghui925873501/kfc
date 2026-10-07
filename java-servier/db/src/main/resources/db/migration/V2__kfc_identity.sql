CREATE TABLE IF NOT EXISTS kfc_installation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    installation_id CHAR(36) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    tdid VARCHAR(128) NULL,
    jpush_reg_id VARCHAR(255) NULL,
    platform VARCHAR(32) NOT NULL,
    app_version VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_kfc_installation_id (installation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kfc_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_user_id BIGINT NOT NULL,
    brand VARCHAR(16) NOT NULL,
    user_code VARCHAR(80) NOT NULL,
    muid VARCHAR(128) NULL,
    suid VARCHAR(128) NULL,
    is_member BOOLEAN NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    last_login_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_kfc_user_brand_user_code (brand, user_code),
    KEY idx_kfc_user_app_user (app_user_id),
    CONSTRAINT fk_kfc_user_app_user FOREIGN KEY (app_user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
