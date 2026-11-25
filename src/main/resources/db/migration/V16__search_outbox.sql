CREATE TABLE search_outbox
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id    VARCHAR(64)   NOT NULL,
    content_type  VARCHAR(24)   NOT NULL,
    content_id    BIGINT UNSIGNED NOT NULL,
    event_version BIGINT UNSIGNED NOT NULL,
    payload       VARCHAR(2000) NOT NULL DEFAULT '',
    status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    retry_count   INT UNSIGNED  NOT NULL DEFAULT 0,
    next_retry_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_owner   VARCHAR(64)            DEFAULT NULL,
    lease_until   DATETIME               DEFAULT NULL,
    sent_at       DATETIME               DEFAULT NULL,
    last_error    VARCHAR(500)           DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_outbox_message (message_id),
    KEY idx_search_outbox_content (content_type, content_id, id),
    KEY idx_search_outbox_scan (status, next_retry_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE search_inbox
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id    VARCHAR(64) NOT NULL,
    status        VARCHAR(16) NOT NULL,
    error_message VARCHAR(500)         DEFAULT NULL,
    processed_at  DATETIME             DEFAULT NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_inbox_message (message_id),
    KEY idx_search_inbox_status_created (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
