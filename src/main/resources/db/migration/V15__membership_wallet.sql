UPDATE resource_version
SET access_type = CASE
    WHEN access_type IN ('GUEST_FREE', 'LOGIN_FREE', 'REPLY_REQUIRED') THEN 'FREE'
    WHEN access_type = 'PAID' THEN 'MEMBER'
    ELSE access_type
END;

ALTER TABLE trade_order
    MODIFY COLUMN resource_id BIGINT UNSIGNED DEFAULT NULL,
    ADD COLUMN order_type VARCHAR(24) NOT NULL DEFAULT 'RESOURCE' AFTER user_id,
    ADD COLUMN membership_plan_id BIGINT UNSIGNED DEFAULT NULL AFTER resource_id,
    ADD COLUMN membership_duration_days INT UNSIGNED DEFAULT NULL AFTER membership_plan_id,
    ADD COLUMN membership_daily_limit INT UNSIGNED DEFAULT NULL AFTER membership_duration_days,
    ADD KEY idx_trade_order_type_user (order_type, user_id, create_time),
    ADD KEY idx_trade_order_membership_plan (membership_plan_id, status);

ALTER TABLE trade_order_item
    MODIFY COLUMN resource_id BIGINT UNSIGNED DEFAULT NULL,
    MODIFY COLUMN owner_id BIGINT UNSIGNED DEFAULT NULL;

CREATE TABLE membership_plan
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(100) NOT NULL,
    description          VARCHAR(500) NOT NULL DEFAULT '',
    duration_days        INT UNSIGNED NOT NULL,
    price                DECIMAL(18, 2) NOT NULL,
    daily_resource_limit INT UNSIGNED NOT NULL,
    status               TINYINT NOT NULL DEFAULT 1,
    sort_order           INT NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_membership_plan_public (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_membership
(
    user_id              BIGINT UNSIGNED NOT NULL,
    plan_id              BIGINT UNSIGNED DEFAULT NULL,
    daily_resource_limit INT UNSIGNED NOT NULL DEFAULT 0,
    expire_at            DATETIME DEFAULT NULL,
    last_order_no        VARCHAR(64) DEFAULT NULL,
    version              INT UNSIGNED NOT NULL DEFAULT 0,
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY idx_user_membership_expire (expire_at, user_id),
    UNIQUE KEY uk_user_membership_order (last_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_daily_usage
(
    user_id     BIGINT UNSIGNED NOT NULL,
    claim_date  DATE NOT NULL,
    used_count  INT UNSIGNED NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, claim_date),
    KEY idx_membership_daily_date (claim_date, used_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_resource_claim
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    claim_date  DATE NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_claim_user_resource (user_id, resource_id),
    KEY idx_membership_claim_user_date (user_id, claim_date, id),
    KEY idx_membership_claim_resource (resource_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_commission_config
(
    id                    BIGINT UNSIGNED NOT NULL,
    points_unit_price     DECIMAL(18, 4) NOT NULL DEFAULT 0.0100,
    member_fixed_amount   DECIMAL(18, 2) NOT NULL DEFAULT 1.00,
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO membership_commission_config(id, points_unit_price, member_fixed_amount)
VALUES (1, 0.0100, 1.00);

CREATE TABLE wallet_account
(
    user_id          BIGINT UNSIGNED NOT NULL,
    available_balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    frozen_balance   DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    total_income     DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    version          INT UNSIGNED NOT NULL DEFAULT 0,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY idx_wallet_available (available_balance, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_ledger
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    biz_type         VARCHAR(32) NOT NULL,
    biz_id           VARCHAR(80) NOT NULL,
    available_change DECIMAL(18, 2) NOT NULL,
    frozen_change    DECIMAL(18, 2) NOT NULL,
    available_after  DECIMAL(18, 2) NOT NULL,
    frozen_after     DECIMAL(18, 2) NOT NULL,
    remark           VARCHAR(500) NOT NULL DEFAULT '',
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_ledger_biz (biz_type, biz_id),
    KEY idx_wallet_ledger_user_created (user_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_withdrawal
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    withdrawal_no     VARCHAR(64) NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    amount            DECIMAL(18, 2) NOT NULL,
    alipay_account    VARCHAR(128) NOT NULL,
    status            VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    external_trade_no VARCHAR(128) DEFAULT NULL,
    handle_note       VARCHAR(1000) DEFAULT NULL,
    handled_by        BIGINT UNSIGNED DEFAULT NULL,
    handled_at        DATETIME DEFAULT NULL,
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_withdrawal_no (withdrawal_no),
    UNIQUE KEY uk_wallet_withdrawal_external (external_trade_no),
    KEY idx_wallet_withdrawal_user_created (user_id, create_time, id),
    KEY idx_wallet_withdrawal_status_created (status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (400, 230, '会员套餐', 'membership:plan', 'MENU', '/trade/membership-plans', 7),
       (401, 400, '会员套餐查询', 'membership:plan:list', 'BUTTON', NULL, 1),
       (402, 400, '会员套餐新增', 'membership:plan:create', 'BUTTON', NULL, 2),
       (403, 400, '会员套餐修改', 'membership:plan:update', 'BUTTON', NULL, 3),
       (404, 400, '返佣配置查询', 'membership:commission:detail', 'BUTTON', NULL, 4),
       (405, 400, '返佣配置修改', 'membership:commission:update', 'BUTTON', NULL, 5),
       (406, 230, '提现管理', 'wallet:withdrawal', 'MENU', '/trade/withdrawals', 8),
       (407, 406, '提现单查询', 'wallet:withdrawal:list', 'BUTTON', NULL, 1),
       (408, 406, '人工确认提现', 'wallet:withdrawal:handle', 'BUTTON', NULL, 2);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 400 AND 408;
