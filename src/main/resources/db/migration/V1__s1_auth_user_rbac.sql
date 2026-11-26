CREATE TABLE user_account
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100)          DEFAULT NULL,
    phone         VARCHAR(20)           DEFAULT NULL,
    password      VARCHAR(100) NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    token_version INT UNSIGNED NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    KEY           idx_user_status_created (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE user_profile
(
    user_id     BIGINT UNSIGNED NOT NULL,
    nickname    VARCHAR(50) NOT NULL,
    avatar      VARCHAR(500)         DEFAULT NULL,
    signature   VARCHAR(255)         DEFAULT NULL,
    birthday    DATE                 DEFAULT NULL,
    gender      TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE auth_refresh_token
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    token_id    VARCHAR(64) NOT NULL,
    user_type   VARCHAR(10) NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    family_id   VARCHAR(64) NOT NULL,
    token_hash  CHAR(64)    NOT NULL,
    expires_at  DATETIME    NOT NULL,
    revoked_at  DATETIME             DEFAULT NULL,
    replaced_by VARCHAR(64)          DEFAULT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_id (token_id),
    UNIQUE KEY uk_refresh_hash (token_hash),
    KEY         idx_refresh_user (user_type, user_id),
    KEY         idx_refresh_family (family_id),
    KEY         idx_refresh_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE sys_admin
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100)          DEFAULT NULL,
    password      VARCHAR(100) NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    token_version INT UNSIGNED NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username),
    UNIQUE KEY uk_admin_email (email),
    KEY           idx_admin_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE sys_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    description VARCHAR(255)         DEFAULT NULL,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code),
    KEY         idx_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE sys_permission
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name        VARCHAR(50)  NOT NULL,
    code        VARCHAR(100) NOT NULL,
    type        VARCHAR(10)  NOT NULL DEFAULT 'BUTTON',
    path        VARCHAR(255)          DEFAULT NULL,
    icon        VARCHAR(100)          DEFAULT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code),
    KEY         idx_permission_parent (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE sys_admin_role
(
    admin_id    BIGINT UNSIGNED NOT NULL,
    role_id     BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id, role_id),
    KEY         idx_admin_role_role (role_id),
    CONSTRAINT fk_admin_role_admin FOREIGN KEY (admin_id) REFERENCES sys_admin (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE sys_role_permission
(
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY           idx_role_permission_permission (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE login_log
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_type   VARCHAR(10)  NOT NULL,
    user_id     BIGINT UNSIGNED DEFAULT NULL,
    account     VARCHAR(100) NOT NULL,
    login_type  VARCHAR(20)  NOT NULL,
    ip          VARCHAR(64)  NOT NULL DEFAULT '',
    location    VARCHAR(100) NOT NULL DEFAULT '未知',
    success     TINYINT      NOT NULL,
    message     VARCHAR(255)          DEFAULT NULL,
    trace_id    VARCHAR(64)           DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY         idx_login_user (user_type, user_id, create_time),
    KEY         idx_login_account (account, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE operation_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_type VARCHAR(10)  NOT NULL DEFAULT 'ANONYMOUS',
    operator_id   BIGINT UNSIGNED DEFAULT NULL,
    operator_name VARCHAR(100) NOT NULL DEFAULT 'anonymous',
    module        VARCHAR(50)  NOT NULL,
    content       VARCHAR(1000)         DEFAULT NULL,
    target_id     VARCHAR(100)          DEFAULT NULL,
    ip            VARCHAR(64)  NOT NULL DEFAULT '',
    location      VARCHAR(100) NOT NULL DEFAULT '未知',
    success       TINYINT      NOT NULL,
    error_message VARCHAR(500)          DEFAULT NULL,
    cost_ms       BIGINT       NOT NULL DEFAULT 0,
    trace_id      VARCHAR(64)           DEFAULT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY           idx_operation_operator (operator_type, operator_id, create_time),
    KEY           idx_operation_module (module, create_time),
    KEY           idx_operation_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_role(id, name, code, description)
VALUES (1, '超级管理员', 'SUPER_ADMIN', '拥有全部系统权限');
INSERT INTO sys_admin(id, username, nickname, password)
VALUES (1, 'admin', '系统管理员', '$2b$12$lJA9ahmb2pCgbHQcUhAwOeS5ODTnvdI3MlPXojRHYU7/8lTPCadlO');
INSERT INTO sys_admin_role(admin_id, role_id)
VALUES (1, 1);
INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (1, 0, '系统管理', 'system', 'MENU', '/system', 1),
       (2, 1, '管理员管理', 'system:admin', 'MENU', '/system/admins', 1),
       (3, 2, '管理员查询', 'system:admin:list', 'BUTTON', NULL, 1),
       (4, 2, '管理员新增', 'system:admin:create', 'BUTTON', NULL, 2),
       (5, 2, '管理员修改', 'system:admin:update', 'BUTTON', NULL, 3),
       (6, 2, '管理员状态', 'system:admin:status', 'BUTTON', NULL, 4),
       (7, 2, '管理员删除', 'system:admin:delete', 'BUTTON', NULL, 5),
       (8, 2, '分配角色', 'system:admin:assign-role', 'BUTTON', NULL, 6),
       (9, 1, '角色管理', 'system:role', 'MENU', '/system/roles', 2),
       (10, 9, '角色查询', 'system:role:list', 'BUTTON', NULL, 1),
       (11, 9, '角色新增', 'system:role:create', 'BUTTON', NULL, 2),
       (12, 9, '角色修改', 'system:role:update', 'BUTTON', NULL, 3),
       (13, 9, '角色状态', 'system:role:status', 'BUTTON', NULL, 4),
       (14, 9, '角色删除', 'system:role:delete', 'BUTTON', NULL, 5),
       (15, 9, '分配权限', 'system:role:assign-permission', 'BUTTON', NULL, 6),
       (16, 1, '权限管理', 'system:permission', 'MENU', '/system/permissions', 3),
       (17, 16, '权限查询', 'system:permission:list', 'BUTTON', NULL, 1),
       (18, 16, '权限新增', 'system:permission:create', 'BUTTON', NULL, 2),
       (19, 16, '权限修改', 'system:permission:update', 'BUTTON', NULL, 3),
       (20, 16, '权限状态', 'system:permission:status', 'BUTTON', NULL, 4),
       (21, 16, '权限删除', 'system:permission:delete', 'BUTTON', NULL, 5),
       (22, 1, '用户管理', 'system:user', 'MENU', '/system/users', 4),
       (23, 22, '用户查询', 'system:user:list', 'BUTTON', NULL, 1),
       (24, 22, '用户状态', 'system:user:status', 'BUTTON', NULL, 2),
       (25, 22, '用户删除', 'system:user:delete', 'BUTTON', NULL, 3),
       (26, 1, '操作日志', 'system:operation-log', 'MENU', '/system/operation-logs', 5),
       (27, 26, '日志查询', 'system:operation-log:list', 'BUTTON', NULL, 1),
       (28, 22, '用户新增', 'system:user:create', 'BUTTON', NULL, 4),
       (29, 22, '用户修改', 'system:user:update', 'BUTTON', NULL, 5);
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id
FROM sys_permission;
