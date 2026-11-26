CREATE TABLE operation_gray_day
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    gray_date   DATE NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '0停用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_operation_gray_day_date (gray_date),
    KEY idx_operation_gray_day_status_date (status, gray_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_background_music
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enabled        TINYINT NOT NULL DEFAULT 0 COMMENT '0关闭，1开启',
    audio_url      VARCHAR(1000) NOT NULL DEFAULT '',
    play_mode      VARCHAR(16) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO自动播放，MANUAL手动播放',
    player_visible TINYINT NOT NULL DEFAULT 1 COMMENT '0隐藏播放器，1显示播放器',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO operation_background_music(id, enabled, audio_url, play_mode, player_visible)
VALUES (1, 0, '', 'AUTO', 1);

CREATE TABLE operation_feedback
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    content        VARCHAR(2000) NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待回复，REPLIED已回复，CLOSED已关闭',
    admin_reply    VARCHAR(2000) DEFAULT NULL,
    reply_admin_id BIGINT UNSIGNED DEFAULT NULL,
    reply_time     DATETIME DEFAULT NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_feedback_user_created (user_id, create_time, id),
    KEY idx_operation_feedback_status_created (status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (500, 300, '灰色日管理', 'operation:gray-day', 'MENU', '/operation/gray-days', 7),
       (501, 500, '灰色日查询', 'operation:gray-day:list', 'BUTTON', NULL, 1),
       (502, 500, '灰色日新增', 'operation:gray-day:create', 'BUTTON', NULL, 2),
       (503, 500, '灰色日修改', 'operation:gray-day:update', 'BUTTON', NULL, 3),
       (504, 500, '灰色日启停', 'operation:gray-day:status', 'BUTTON', NULL, 4),
       (505, 300, '背景音乐', 'operation:music', 'MENU', '/operation/background-music', 8),
       (506, 505, '音乐配置查询', 'operation:music:list', 'BUTTON', NULL, 1),
       (507, 505, '音乐配置修改', 'operation:music:update', 'BUTTON', NULL, 2),
       (508, 300, '意见反馈', 'operation:feedback', 'MENU', '/operation/feedbacks', 9),
       (509, 508, '反馈查询', 'operation:feedback:list', 'BUTTON', NULL, 1),
       (510, 508, '反馈回复', 'operation:feedback:reply', 'BUTTON', NULL, 2),
       (511, 508, '反馈关闭', 'operation:feedback:close', 'BUTTON', NULL, 3);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 500 AND 511;
