ALTER TABLE user_profile
    ADD COLUMN avatar_file_id BIGINT UNSIGNED DEFAULT NULL AFTER avatar,
    ADD COLUMN pending_nickname VARCHAR(50) DEFAULT NULL AFTER avatar_file_id,
    ADD COLUMN pending_avatar_file_id BIGINT UNSIGNED DEFAULT NULL AFTER pending_nickname,
    ADD COLUMN pending_signature VARCHAR(255) DEFAULT NULL AFTER pending_avatar_file_id,
    ADD COLUMN pending_remove_avatar TINYINT NOT NULL DEFAULT 0 AFTER pending_signature,
    ADD COLUMN profile_audit_status VARCHAR(24) NOT NULL DEFAULT 'APPROVED' AFTER pending_remove_avatar,
    ADD COLUMN profile_audit_reason VARCHAR(500) DEFAULT NULL AFTER profile_audit_status,
    ADD COLUMN profile_version INT UNSIGNED NOT NULL DEFAULT 0 AFTER profile_audit_reason,
    ADD KEY idx_profile_audit_status (profile_audit_status, update_time);
