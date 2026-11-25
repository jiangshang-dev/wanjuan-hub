CREATE TABLE resource_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    status      TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_category_name (name),
    KEY idx_resource_category_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_tag
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_info
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_id            BIGINT UNSIGNED NOT NULL,
    category_id          BIGINT UNSIGNED NOT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    latest_version_no    INT UNSIGNED NOT NULL,
    published_version_no INT UNSIGNED DEFAULT NULL,
    audit_reason         VARCHAR(500) DEFAULT NULL,
    online               TINYINT NOT NULL DEFAULT 0,
    invalid              TINYINT NOT NULL DEFAULT 0,
    view_count           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted              TINYINT NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resource_author_created (author_id, deleted, create_time),
    KEY idx_resource_public (deleted, online, invalid, published_version_no, category_id),
    KEY idx_resource_status_updated (status, deleted, update_time),
    CONSTRAINT fk_resource_category FOREIGN KEY (category_id) REFERENCES resource_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_version
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id         BIGINT UNSIGNED NOT NULL,
    version_no          INT UNSIGNED NOT NULL,
    title               VARCHAR(120) NOT NULL,
    summary             VARCHAR(500) NOT NULL,
    description         MEDIUMTEXT NOT NULL,
    cover_file_id       BIGINT UNSIGNED DEFAULT NULL,
    attachment_file_ids VARCHAR(4000) NOT NULL DEFAULT '[]',
    access_type         VARCHAR(24) NOT NULL,
    points_price        INT UNSIGNED DEFAULT NULL,
    price               DECIMAL(18, 2) DEFAULT NULL,
    copyright_notice    VARCHAR(1000) NOT NULL,
    status              VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    audit_reason        VARCHAR(500) DEFAULT NULL,
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_version (resource_id, version_no),
    KEY idx_resource_version_status (resource_id, status),
    CONSTRAINT fk_resource_version_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_disk_type
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(32) NOT NULL,
    name          VARCHAR(50) NOT NULL,
    allowed_hosts VARCHAR(2000) NOT NULL,
    sort_order    INT NOT NULL DEFAULT 0,
    status        TINYINT NOT NULL DEFAULT 1,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_disk_code (code),
    KEY idx_resource_disk_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_link
(
    id                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id               BIGINT UNSIGNED NOT NULL,
    version_no                INT UNSIGNED NOT NULL,
    disk_type_id              BIGINT UNSIGNED NOT NULL,
    url_ciphertext            TEXT NOT NULL,
    extraction_code_ciphertext VARCHAR(512) DEFAULT NULL,
    link_name                 VARCHAR(100) NOT NULL,
    status                    VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    sort_order                INT NOT NULL DEFAULT 0,
    create_time               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resource_link_version (resource_id, version_no, status, sort_order, id),
    CONSTRAINT fk_resource_link_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id),
    CONSTRAINT fk_resource_link_disk FOREIGN KEY (disk_type_id) REFERENCES resource_disk_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_tag_relation
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id BIGINT UNSIGNED NOT NULL,
    tag_id      BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_tag_relation (resource_id, tag_id),
    KEY idx_resource_tag_relation_tag (tag_id, resource_id),
    CONSTRAINT fk_resource_tag_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id),
    CONSTRAINT fk_resource_tag_tag FOREIGN KEY (tag_id) REFERENCES resource_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_resource_relation
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(24) NOT NULL,
    content_id   BIGINT UNSIGNED NOT NULL,
    resource_id  BIGINT UNSIGNED NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_resource (content_type, content_id, resource_id),
    KEY idx_content_resource_resource (resource_id, content_type, content_id),
    CONSTRAINT fk_content_resource_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_entitlement
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    resource_id    BIGINT UNSIGNED NOT NULL,
    source_type    VARCHAR(16) NOT NULL,
    source_order_no VARCHAR(64) DEFAULT NULL,
    amount         DECIMAL(18, 2) DEFAULT NULL,
    points_amount  INT UNSIGNED DEFAULT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    revoke_reason  VARCHAR(500) DEFAULT NULL,
    granted_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at     DATETIME DEFAULT NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_entitlement_user_resource (user_id, resource_id),
    UNIQUE KEY uk_resource_entitlement_order (source_type, source_order_no),
    KEY idx_resource_entitlement_resource (resource_id, status, user_id),
    KEY idx_resource_entitlement_user_created (user_id, status, create_time),
    CONSTRAINT fk_resource_entitlement_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_access_record
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED DEFAULT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resource_access_user (user_id, resource_id, create_time),
    KEY idx_resource_access_resource (resource_id, target_type, target_id, create_time),
    CONSTRAINT fk_resource_access_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_invalid_report
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id BIGINT UNSIGNED NOT NULL,
    link_id     BIGINT UNSIGNED DEFAULT NULL,
    reporter_id BIGINT UNSIGNED NOT NULL,
    reason      VARCHAR(1000) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    handler_id  BIGINT UNSIGNED DEFAULT NULL,
    handle_note VARCHAR(1000) DEFAULT NULL,
    handled_at  DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_resource_invalid_status (status, create_time, id),
    KEY idx_resource_invalid_resource (resource_id, status, create_time),
    KEY idx_resource_invalid_reporter (reporter_id, create_time),
    CONSTRAINT fk_resource_invalid_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id),
    CONSTRAINT fk_resource_invalid_link FOREIGN KEY (link_id) REFERENCES resource_link (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_point_account
(
    user_id     BIGINT UNSIGNED NOT NULL,
    balance     INT UNSIGNED NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY idx_resource_point_balance (balance, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_point_transaction
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    resource_id    BIGINT UNSIGNED NOT NULL,
    change_amount  INT NOT NULL,
    balance_after  INT UNSIGNED NOT NULL,
    biz_type       VARCHAR(24) NOT NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_point_biz (user_id, biz_type, resource_id),
    KEY idx_resource_point_created (user_id, create_time, id),
    CONSTRAINT fk_resource_point_resource FOREIGN KEY (resource_id) REFERENCES resource_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO resource_disk_type(code, name, allowed_hosts, sort_order)
VALUES ('BAIDU', '百度网盘', 'pan.baidu.com', 1),
       ('QUARK', '夸克网盘', 'pan.quark.cn', 2),
       ('ALIPAN', '阿里云盘', 'www.alipan.com,www.aliyundrive.com', 3),
       ('PAN123', '123云盘', 'www.123pan.com,www.123684.com', 4),
       ('CT189', '天翼云盘', 'cloud.189.cn', 5);
