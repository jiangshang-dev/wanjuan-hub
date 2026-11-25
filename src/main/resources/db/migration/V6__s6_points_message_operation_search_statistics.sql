CREATE TABLE points_account
(
    user_id      BIGINT UNSIGNED NOT NULL,
    balance      INT UNSIGNED NOT NULL DEFAULT 0,
    total_points BIGINT UNSIGNED NOT NULL DEFAULT 0,
    experience   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    version      INT UNSIGNED NOT NULL DEFAULT 0,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY idx_points_account_experience (experience, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE points_record
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    change_amount    INT NOT NULL,
    balance_after    INT UNSIGNED NOT NULL,
    experience_change INT NOT NULL DEFAULT 0,
    experience_after BIGINT UNSIGNED NOT NULL DEFAULT 0,
    biz_type         VARCHAR(40) NOT NULL,
    biz_id           BIGINT UNSIGNED DEFAULT NULL,
    idempotency_key  VARCHAR(160) NOT NULL,
    remark           VARCHAR(500) NOT NULL DEFAULT '',
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_record_idempotency (idempotency_key),
    KEY idx_points_record_user_created (user_id, create_time, id),
    KEY idx_points_record_biz (user_id, biz_type, biz_id),
    CONSTRAINT fk_points_record_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE points_rule
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code         VARCHAR(40) NOT NULL,
    name         VARCHAR(80) NOT NULL,
    points       INT NOT NULL,
    experience   INT NOT NULL DEFAULT 0,
    daily_limit  INT NOT NULL DEFAULT 0 COMMENT '0表示不限制',
    status       TINYINT NOT NULL DEFAULT 1,
    remark       VARCHAR(500) NOT NULL DEFAULT '',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_rule_code (code),
    KEY idx_points_rule_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_sign_in
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    sign_date       DATE NOT NULL,
    points          INT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sign_in_date (user_id, sign_date),
    UNIQUE KEY uk_user_sign_in_idempotency (idempotency_key),
    CONSTRAINT fk_user_sign_in_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_level
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    level          INT UNSIGNED NOT NULL,
    name           VARCHAR(50) NOT NULL,
    min_experience BIGINT UNSIGNED NOT NULL,
    icon           VARCHAR(500) DEFAULT NULL,
    benefits       VARCHAR(1000) NOT NULL DEFAULT '',
    status         TINYINT NOT NULL DEFAULT 1,
    sort_order     INT NOT NULL DEFAULT 0,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_level_level (level),
    UNIQUE KEY uk_user_level_experience (min_experience),
    KEY idx_user_level_status_sort (status, sort_order, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message_notification
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    type        VARCHAR(24) NOT NULL,
    title       VARCHAR(120) NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    biz_type    VARCHAR(40) DEFAULT NULL,
    biz_id      VARCHAR(100) DEFAULT NULL,
    dedup_key   VARCHAR(160) NOT NULL,
    read_flag   TINYINT NOT NULL DEFAULT 0,
    read_time   DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_user_dedup (user_id, dedup_key),
    KEY idx_message_user_read_created (user_id, read_flag, create_time, id),
    CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message_preference
(
    user_id            BIGINT UNSIGNED NOT NULL,
    audit_enabled      TINYINT NOT NULL DEFAULT 1,
    payment_enabled    TINYINT NOT NULL DEFAULT 1,
    system_enabled     TINYINT NOT NULL DEFAULT 1,
    interaction_enabled TINYINT NOT NULL DEFAULT 1,
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_message_preference_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_notice
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title        VARCHAR(120) NOT NULL,
    content      TEXT NOT NULL,
    status       TINYINT NOT NULL DEFAULT 1,
    publish_start DATETIME DEFAULT NULL,
    publish_end  DATETIME DEFAULT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_notice_public (status, publish_start, publish_end, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_recommend
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(24) NOT NULL,
    content_id   BIGINT UNSIGNED NOT NULL,
    title        VARCHAR(120) NOT NULL,
    reason       VARCHAR(500) NOT NULL DEFAULT '',
    status       TINYINT NOT NULL DEFAULT 1,
    sort_order   INT NOT NULL DEFAULT 0,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_operation_recommend_content (content_type, content_id),
    KEY idx_operation_recommend_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_advertisement
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title       VARCHAR(120) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    target_url  VARCHAR(500) NOT NULL,
    position    VARCHAR(40) NOT NULL,
    status      TINYINT NOT NULL DEFAULT 1,
    start_time  DATETIME DEFAULT NULL,
    end_time    DATETIME DEFAULT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_ad_public (position, status, start_time, end_time, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_friend_link
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    logo_url    VARCHAR(500) DEFAULT NULL,
    status      TINYINT NOT NULL DEFAULT 1,
    sort_order  INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_operation_friend_link_name (name),
    KEY idx_operation_friend_link_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE search_hot_keyword
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    keyword     VARCHAR(100) NOT NULL,
    heat        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1,
    sort_order  INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_hot_keyword (keyword),
    KEY idx_search_hot_rank (status, sort_order, heat, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_config
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key  VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    public_flag TINYINT NOT NULL DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_key (config_key),
    KEY idx_sys_config_public (public_flag, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE statistics_daily
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    stat_date    DATE NOT NULL,
    new_users    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    new_contents BIGINT UNSIGNED NOT NULL DEFAULT 0,
    audit_count  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    order_count  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    order_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_statistics_daily_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE search_document
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(24) NOT NULL,
    content_id   BIGINT UNSIGNED NOT NULL,
    title        VARCHAR(255) NOT NULL,
    summary      TEXT NOT NULL,
    content      MEDIUMTEXT NOT NULL,
    url          VARCHAR(500) NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_document_public (content_type, content_id),
    FULLTEXT KEY ft_search_document_ngram (title, summary, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO points_rule(code, name, points, experience, daily_limit, remark) VALUES
('SIGN_IN', '每日签到', 10, 10, 10, '每日首次签到奖励'),
('BLOG_ARTICLE', '博客审核通过', 20, 20, 100, '按审核任务幂等发放'),
('RESOURCE', '资源审核通过', 30, 30, 150, '按审核任务幂等发放'),
('FORUM_TOPIC', '论坛主题审核通过', 15, 15, 75, '按审核任务幂等发放');

INSERT INTO user_level(level, name, min_experience, benefits, sort_order) VALUES
(1, '初识万卷', 0, '基础浏览与互动', 1),
(2, '勤学成员', 100, '成长标识', 2),
(3, '资深成员', 500, '资深成长标识', 3),
(4, '万卷达人', 2000, '达人成长标识', 4),
(5, '万卷之星', 10000, '星级成长标识', 5);

INSERT INTO sys_config(config_key, config_value, description, public_flag) VALUES
('site.name', '万卷汇', '网站名称', 1),
('site.userAgreement', '请遵守法律法规与社区规范，仅分享有权发布的内容。', '用户协议', 1),
('site.privacyPolicy', '我们仅在提供社区服务所需范围内处理账号与使用数据。', '隐私政策', 1),
('site.icp', '', 'ICP备案号', 1),
('site.copyright', '万卷汇', '版权信息', 1);

DELIMITER $$
CREATE TRIGGER trg_points_record_immutable_update BEFORE UPDATE ON points_record
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'points_record is immutable';
END$$
CREATE TRIGGER trg_points_record_immutable_delete BEFORE DELETE ON points_record
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'points_record is immutable';
END$$
DELIMITER ;
