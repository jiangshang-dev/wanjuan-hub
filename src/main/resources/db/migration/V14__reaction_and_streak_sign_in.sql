ALTER TABLE user_profile
    ADD COLUMN allow_like TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许他人点赞自己的内容' AFTER gender,
    ADD COLUMN allow_dislike TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许他人点踩自己的内容' AFTER allow_like;

ALTER TABLE content_like
    ADD COLUMN reaction_type VARCHAR(16) NOT NULL DEFAULT 'LIKE' COMMENT 'LIKE点赞/DISLIKE点踩' AFTER target_id,
    ADD KEY idx_reaction_target_type (target_type, target_id, reaction_type, user_id);

ALTER TABLE blog_article ADD COLUMN dislike_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER like_count;
ALTER TABLE forum_topic ADD COLUMN dislike_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER like_count;
ALTER TABLE forum_post ADD COLUMN dislike_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER like_count;
ALTER TABLE content_comment ADD COLUMN dislike_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER like_count;

ALTER TABLE user_sign_in
    ADD COLUMN streak_days INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '签到当天连续天数' AFTER sign_date;

CREATE TABLE sign_in_streak_rule
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    streak_day   INT UNSIGNED NOT NULL COMMENT '连续签到第N天',
    points       INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该档总奖励积分',
    experience   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该档总奖励经验',
    status       TINYINT NOT NULL DEFAULT 1,
    remark       VARCHAR(500) NOT NULL DEFAULT '',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sign_in_streak_day (streak_day),
    KEY idx_sign_in_streak_status_day (status, streak_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sign_in_streak_rule(streak_day, points, experience, remark) VALUES
(1, 10, 10, '连续签到第1天'),
(2, 12, 12, '连续签到第2天'),
(3, 15, 15, '连续签到第3天'),
(4, 20, 20, '连续签到第4天'),
(5, 25, 25, '连续签到第5天'),
(6, 30, 30, '连续签到第6天'),
(7, 50, 50, '连续签到第7天及以后沿用该档');

INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (393, 371, '连续签到档位查询', 'points:sign-in-rule:list', 'BUTTON', NULL, 3),
       (394, 371, '连续签到档位新增', 'points:sign-in-rule:create', 'BUTTON', NULL, 4),
       (395, 371, '连续签到档位修改', 'points:sign-in-rule:update', 'BUTTON', NULL, 5),
       (396, 371, '连续签到档位删除', 'points:sign-in-rule:delete', 'BUTTON', NULL, 6);
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 393 AND 396;
