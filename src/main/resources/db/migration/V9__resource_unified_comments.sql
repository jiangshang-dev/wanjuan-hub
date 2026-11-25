ALTER TABLE resource_info
    ADD COLUMN comment_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER view_count;
