-- 先移除外键，才能给被引用的 app_user.id 和引用方 app_user_id 补列注释。
ALTER TABLE kfc_user DROP FOREIGN KEY fk_kfc_user_app_user;

ALTER TABLE app_user
    COMMENT = '本地应用用户，不在此表保存手机号明文',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '本地用户主键',
    MODIFY COLUMN nickname VARCHAR(80) NOT NULL COMMENT '本地应用展示昵称',
    MODIFY COLUMN phone_hash CHAR(64) NULL COMMENT '手机号哈希值，用于本地账号去重',
    MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '用户记录创建时间',
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '用户记录最近更新时间';

ALTER TABLE kfc_installation
    COMMENT = '本应用一次安装的稳定设备上下文，登录前即可建立',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '安装记录主键',
    MODIFY COLUMN installation_id CHAR(36) NOT NULL COMMENT '后端首次注册时生成的安装标识',
    MODIFY COLUMN device_id VARCHAR(128) NOT NULL COMMENT '当前应用设备 API 返回的设备标识',
    MODIFY COLUMN tdid VARCHAR(128) NULL COMMENT 'TalkingData SDK 实际返回的设备追踪标识',
    MODIFY COLUMN jpush_reg_id VARCHAR(255) NULL COMMENT '推送 SDK 实际返回的注册标识',
    MODIFY COLUMN platform VARCHAR(32) NOT NULL COMMENT '当前应用运行平台',
    MODIFY COLUMN app_version VARCHAR(64) NULL COMMENT '当前应用版本',
    MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '安装记录创建时间',
    MODIFY COLUMN last_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近一次确认安装上下文的时间';

ALTER TABLE kfc_user
    COMMENT = '本地用户与 KFC 账号映射及最近一次登录凭据',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'KFC 用户映射主键',
    MODIFY COLUMN app_user_id BIGINT NOT NULL COMMENT '关联本地 app_user.id',
    MODIFY COLUMN brand VARCHAR(16) NOT NULL COMMENT '上游品牌标识',
    MODIFY COLUMN user_code VARCHAR(80) NOT NULL COMMENT 'KFC 返回的用户编码',
    MODIFY COLUMN muid VARCHAR(128) NULL COMMENT 'KFC 返回的会员标识',
    MODIFY COLUMN suid VARCHAR(128) NULL COMMENT 'KFC 返回的身份标识',
    MODIFY COLUMN is_member BOOLEAN NULL COMMENT 'KFC 返回的会员状态',
    MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '映射记录创建时间',
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '映射记录最近更新时间',
    MODIFY COLUMN last_login_at DATETIME(3) NOT NULL COMMENT '最近一次成功登录时间',
    MODIFY COLUMN phone_plain VARCHAR(32) NULL COMMENT '最近一次登录手机号明文，仅供授权数据库用户查看',
    MODIFY COLUMN phone_ciphertext VARCHAR(512) NULL COMMENT '同一手机号发送给上游的 DES 密文',
    MODIFY COLUMN token_plain TEXT NULL COMMENT '最近一次成功登录返回的上游 token 明文',
    MODIFY COLUMN token_ciphertext TEXT NULL COMMENT '同一上游 token 的本地 AES-GCM 密文',
    MODIFY COLUMN token_updated_at DATETIME(3) NULL COMMENT '最近一次更新上游 token 的时间',
    ADD CONSTRAINT fk_kfc_user_app_user FOREIGN KEY (app_user_id) REFERENCES app_user (id);
