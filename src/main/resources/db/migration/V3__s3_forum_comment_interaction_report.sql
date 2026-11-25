CREATE TABLE forum_section
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_forum_section_name (name),
    KEY idx_section_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_board
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    section_id       BIGINT UNSIGNED NOT NULL,
    parent_id        BIGINT UNSIGNED DEFAULT NULL,
    name             VARCHAR(50) NOT NULL,
    description      VARCHAR(500) DEFAULT NULL,
    sort_order       INT NOT NULL DEFAULT 0,
    status           TINYINT NOT NULL DEFAULT 1,
    post_permission  VARCHAR(16) NOT NULL DEFAULT 'USER',
    reply_permission VARCHAR(16) NOT NULL DEFAULT 'USER',
    topic_count      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_board_parent_name (section_id, parent_id, name),
    KEY idx_board_public (status, section_id, parent_id, sort_order, id),
    KEY idx_board_parent (parent_id, status),
    CONSTRAINT fk_board_section FOREIGN KEY (section_id) REFERENCES forum_section (id),
    CONSTRAINT fk_board_parent FOREIGN KEY (parent_id) REFERENCES forum_board (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_moderator
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    board_id    BIGINT UNSIGNED NOT NULL,
    admin_id    BIGINT UNSIGNED NOT NULL,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_moderator_board_admin (board_id, admin_id),
    KEY idx_moderator_admin_board (admin_id, status, board_id),
    KEY idx_moderator_board_status (board_id, status, admin_id),
    CONSTRAINT fk_moderator_board FOREIGN KEY (board_id) REFERENCES forum_board (id),
    CONSTRAINT fk_moderator_admin FOREIGN KEY (admin_id) REFERENCES sys_admin (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_topic
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    board_id             BIGINT UNSIGNED NOT NULL,
    author_id            BIGINT UNSIGNED NOT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    latest_version_no    INT UNSIGNED NOT NULL DEFAULT 1,
    published_version_no INT UNSIGNED DEFAULT NULL,
    audit_reason         VARCHAR(500) DEFAULT NULL,
    locked               TINYINT NOT NULL DEFAULT 0,
    topped               TINYINT NOT NULL DEFAULT 0,
    essence              TINYINT NOT NULL DEFAULT 0,
    post_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    comment_count        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    like_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    favorite_count       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    view_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted              TINYINT NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_topic_public (deleted, status, board_id, topped, id),
    KEY idx_topic_board_created (board_id, deleted, create_time, id),
    KEY idx_topic_author_created (author_id, deleted, create_time, id),
    KEY idx_topic_status_updated (status, deleted, update_time),
    CONSTRAINT fk_topic_board FOREIGN KEY (board_id) REFERENCES forum_board (id),
    CONSTRAINT fk_topic_author FOREIGN KEY (author_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_topic_version
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    topic_id         BIGINT UNSIGNED NOT NULL,
    version_no       INT UNSIGNED NOT NULL,
    title            VARCHAR(120) NOT NULL,
    content          MEDIUMTEXT NOT NULL,
    content_file_ids VARCHAR(2000) NOT NULL DEFAULT '[]',
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    audit_reason     VARCHAR(500) DEFAULT NULL,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_version (topic_id, version_no),
    KEY idx_topic_version_status (topic_id, status, version_no),
    CONSTRAINT fk_topic_version_topic FOREIGN KEY (topic_id) REFERENCES forum_topic (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_post
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    topic_id             BIGINT UNSIGNED NOT NULL,
    board_id             BIGINT UNSIGNED NOT NULL,
    author_id            BIGINT UNSIGNED NOT NULL,
    root_id              BIGINT UNSIGNED DEFAULT NULL,
    parent_id            BIGINT UNSIGNED DEFAULT NULL,
    reply_to_id          BIGINT UNSIGNED DEFAULT NULL,
    floor_no             INT UNSIGNED NOT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    latest_version_no    INT UNSIGNED NOT NULL DEFAULT 1,
    published_version_no INT UNSIGNED DEFAULT NULL,
    audit_reason         VARCHAR(500) DEFAULT NULL,
    like_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted              TINYINT NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_topic_floor (topic_id, floor_no),
    KEY idx_post_topic_public (topic_id, deleted, status, floor_no, id),
    KEY idx_post_topic_author (topic_id, author_id, deleted, floor_no),
    KEY idx_post_author_created (author_id, deleted, create_time, id),
    KEY idx_post_parent (parent_id, deleted, id),
    KEY idx_post_root (root_id, deleted, floor_no),
    CONSTRAINT fk_post_topic FOREIGN KEY (topic_id) REFERENCES forum_topic (id),
    CONSTRAINT fk_post_board FOREIGN KEY (board_id) REFERENCES forum_board (id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES user_account (id),
    CONSTRAINT fk_post_parent FOREIGN KEY (parent_id) REFERENCES forum_post (id),
    CONSTRAINT fk_post_reply_to FOREIGN KEY (reply_to_id) REFERENCES forum_post (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE forum_post_version
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id          BIGINT UNSIGNED NOT NULL,
    version_no       INT UNSIGNED NOT NULL,
    content          MEDIUMTEXT NOT NULL,
    content_file_ids VARCHAR(2000) NOT NULL DEFAULT '[]',
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    audit_reason     VARCHAR(500) DEFAULT NULL,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_version (post_id, version_no),
    KEY idx_post_version_status (post_id, status, version_no),
    CONSTRAINT fk_post_version_post FOREIGN KEY (post_id) REFERENCES forum_post (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_comment
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    target_type          VARCHAR(16) NOT NULL,
    target_id            BIGINT UNSIGNED NOT NULL,
    author_id            BIGINT UNSIGNED NOT NULL,
    root_id              BIGINT UNSIGNED DEFAULT NULL,
    parent_id            BIGINT UNSIGNED DEFAULT NULL,
    reply_to_id          BIGINT UNSIGNED DEFAULT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    latest_version_no    INT UNSIGNED NOT NULL DEFAULT 1,
    published_version_no INT UNSIGNED DEFAULT NULL,
    audit_reason         VARCHAR(500) DEFAULT NULL,
    reply_count          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    like_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted              TINYINT NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comment_target_root (target_type, target_id, root_id, deleted, status, id),
    KEY idx_comment_root_replies (root_id, deleted, status, id),
    KEY idx_comment_author_created (author_id, deleted, create_time, id),
    KEY idx_comment_parent (parent_id, deleted, id),
    KEY idx_comment_admin (status, deleted, create_time, id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES user_account (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES content_comment (id),
    CONSTRAINT fk_comment_reply_to FOREIGN KEY (reply_to_id) REFERENCES content_comment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_comment_version
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    comment_id       BIGINT UNSIGNED NOT NULL,
    version_no       INT UNSIGNED NOT NULL,
    content          TEXT NOT NULL,
    content_file_ids VARCHAR(2000) NOT NULL DEFAULT '[]',
    status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    audit_reason     VARCHAR(500) DEFAULT NULL,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_version (comment_id, version_no),
    KEY idx_comment_version_status (comment_id, status, version_no),
    CONSTRAINT fk_comment_version_comment FOREIGN KEY (comment_id) REFERENCES content_comment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_like
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_user_target (user_id, target_type, target_id),
    KEY idx_like_user_created (user_id, create_time, id),
    KEY idx_like_target (target_type, target_id, user_id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_favorite
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_target (user_id, target_type, target_id),
    KEY idx_favorite_user_created (user_id, create_time, id),
    KEY idx_favorite_target (target_type, target_id, user_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_follow
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    follower_id BIGINT UNSIGNED NOT NULL,
    followee_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow_pair (follower_id, followee_id),
    KEY idx_follow_follower_created (follower_id, create_time, id),
    KEY idx_follow_followee_created (followee_id, create_time, id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user_account (id),
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_report
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT UNSIGNED NOT NULL,
    reason      VARCHAR(50) NOT NULL,
    detail      VARCHAR(1000) DEFAULT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_status_created (status, create_time, id),
    KEY idx_report_reporter_created (reporter_id, create_time, id),
    KEY idx_report_target (target_type, target_id, status),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_report_handle
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    report_id   BIGINT UNSIGNED NOT NULL,
    handler_id  BIGINT UNSIGNED NOT NULL,
    action      VARCHAR(16) NOT NULL,
    note        VARCHAR(1000) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_handle_report (report_id),
    KEY idx_report_handle_handler (handler_id, create_time),
    CONSTRAINT fk_report_handle_report FOREIGN KEY (report_id) REFERENCES content_report (id),
    CONSTRAINT fk_report_handle_admin FOREIGN KEY (handler_id) REFERENCES sys_admin (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE blog_article
    ADD COLUMN like_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER view_count,
    ADD COLUMN favorite_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER like_count,
    ADD COLUMN comment_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER favorite_count;
