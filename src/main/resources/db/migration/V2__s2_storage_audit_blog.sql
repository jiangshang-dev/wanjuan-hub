CREATE TABLE file_object
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id          BIGINT UNSIGNED NOT NULL,
    file_type         VARCHAR(16)  NOT NULL,
    bucket_name       VARCHAR(128) NOT NULL,
    object_key        VARCHAR(500) NOT NULL,
    public_bucket     VARCHAR(128)          DEFAULT NULL,
    public_object_key VARCHAR(500)          DEFAULT NULL,
    original_name     VARCHAR(255) NOT NULL,
    extension         VARCHAR(16)  NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    size_bytes        BIGINT UNSIGNED NOT NULL,
    sha256            CHAR(64)     NOT NULL,
    visibility        VARCHAR(16)  NOT NULL DEFAULT 'PRIVATE',
    audit_status      VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
    audit_reason      VARCHAR(500)          DEFAULT NULL,
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_bucket_key (bucket_name, object_key),
    KEY               idx_file_sha256 (sha256),
    KEY               idx_file_owner_created (owner_id, create_time),
    KEY               idx_file_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE file_reference
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    file_id     BIGINT UNSIGNED NOT NULL,
    biz_type    VARCHAR(32) NOT NULL,
    biz_id      BIGINT UNSIGNED NOT NULL,
    version_no  INT UNSIGNED NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_reference (file_id, biz_type, biz_id, version_no),
    KEY         idx_reference_biz (biz_type, biz_id, version_no),
    CONSTRAINT fk_reference_file FOREIGN KEY (file_id) REFERENCES file_object (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE audit_task
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_type    VARCHAR(32) NOT NULL,
    biz_id      BIGINT UNSIGNED NOT NULL,
    author_id   BIGINT UNSIGNED DEFAULT NULL,
    version_no  INT UNSIGNED NOT NULL,
    status      VARCHAR(24) NOT NULL,
    reason      VARCHAR(500)         DEFAULT NULL,
    retry_count INT UNSIGNED NOT NULL DEFAULT 0,
    lease_owner VARCHAR(64)          DEFAULT NULL,
    lease_until DATETIME             DEFAULT NULL,
    finished_at DATETIME             DEFAULT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_audit_biz_version (biz_type, biz_id, version_no),
    KEY         idx_audit_status_created (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE audit_item
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_id             BIGINT UNSIGNED NOT NULL,
    item_type           VARCHAR(16) NOT NULL,
    content             MEDIUMTEXT  NOT NULL,
    status              VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    suggestion          VARCHAR(32)          DEFAULT NULL,
    reason              VARCHAR(500)         DEFAULT NULL,
    provider_request_id VARCHAR(128)         DEFAULT NULL,
    create_time         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY                 idx_audit_item_task (task_id),
    CONSTRAINT fk_audit_item_task FOREIGN KEY (task_id) REFERENCES audit_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE audit_manual_record
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_id     BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL,
    decision    VARCHAR(16)  NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY         idx_manual_task_created (task_id, create_time),
    CONSTRAINT fk_manual_task FOREIGN KEY (task_id) REFERENCES audit_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE audit_outbox
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id    VARCHAR(64)   NOT NULL,
    task_id       BIGINT UNSIGNED NOT NULL,
    payload       VARCHAR(1000) NOT NULL,
    status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    retry_count   INT UNSIGNED NOT NULL DEFAULT 0,
    next_retry_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_owner   VARCHAR(64)            DEFAULT NULL,
    lease_until   DATETIME               DEFAULT NULL,
    sent_at       DATETIME               DEFAULT NULL,
    last_error    VARCHAR(500)           DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_message (message_id),
    UNIQUE KEY uk_outbox_task (task_id),
    KEY           idx_outbox_scan (status, next_retry_at, lease_until),
    CONSTRAINT fk_outbox_task FOREIGN KEY (task_id) REFERENCES audit_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE mq_consume_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id    VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    status        VARCHAR(16) NOT NULL,
    error_message VARCHAR(500)         DEFAULT NULL,
    processed_at  DATETIME             DEFAULT NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_message (consumer_name, message_id),
    KEY           idx_consume_status_created (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE blog_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255)         DEFAULT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_category_name (name),
    KEY         idx_blog_category_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE blog_tag
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE blog_article
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_id            BIGINT UNSIGNED NOT NULL,
    category_id          BIGINT UNSIGNED NOT NULL,
    original_type        VARCHAR(16) NOT NULL DEFAULT 'ORIGINAL',
    source_name          VARCHAR(100)         DEFAULT NULL,
    source_url           VARCHAR(500)         DEFAULT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    latest_version_no    INT UNSIGNED NOT NULL,
    published_version_no INT UNSIGNED DEFAULT NULL,
    audit_reason         VARCHAR(500)         DEFAULT NULL,
    view_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted              TINYINT     NOT NULL DEFAULT 0,
    create_time          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY                  idx_article_author_created (author_id, create_time),
    KEY                  idx_article_public (deleted, status, published_version_no, category_id),
    KEY                  idx_article_status_updated (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE blog_article_version
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    article_id       BIGINT UNSIGNED NOT NULL,
    version_no       INT UNSIGNED NOT NULL,
    title            VARCHAR(120)  NOT NULL,
    summary          VARCHAR(500)  NOT NULL,
    content          MEDIUMTEXT    NOT NULL,
    cover_file_id    BIGINT UNSIGNED DEFAULT NULL,
    content_file_ids VARCHAR(2000) NOT NULL DEFAULT '[]',
    status           VARCHAR(24)   NOT NULL DEFAULT 'DRAFT',
    audit_reason     VARCHAR(500)           DEFAULT NULL,
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_version (article_id, version_no),
    KEY              idx_version_status (status, article_id),
    CONSTRAINT fk_version_article FOREIGN KEY (article_id) REFERENCES blog_article (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE blog_article_tag
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    article_id  BIGINT UNSIGNED NOT NULL,
    tag_id      BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_tag (article_id, tag_id),
    KEY         idx_article_tag_tag (tag_id, article_id),
    CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_article_tag_tag FOREIGN KEY (tag_id) REFERENCES blog_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
