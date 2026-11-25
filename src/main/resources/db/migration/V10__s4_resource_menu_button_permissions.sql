INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (190, 0, '资源管理', 'resource', 'MENU', '/resource', 7),
       (191, 190, '资源列表', 'resource:manage', 'MENU', '/resource/list', 1),
       (192, 191, '资源查询', 'resource:list', 'BUTTON', NULL, 1),
       (193, 191, '资源状态', 'resource:status', 'BUTTON', NULL, 2),
       (194, 190, '失效举报', 'resource:report', 'MENU', '/resource/reports', 2),
       (195, 194, '举报查询', 'resource:report:list', 'BUTTON', NULL, 1),
       (196, 194, '举报处理', 'resource:report:handle', 'BUTTON', NULL, 2),
       (197, 190, '资源分类', 'resource:category', 'MENU', '/resource/categories', 3),
       (198, 197, '分类查询', 'resource:category:list', 'BUTTON', NULL, 1),
       (199, 197, '分类新增', 'resource:category:create', 'BUTTON', NULL, 2),
       (200, 197, '分类修改', 'resource:category:update', 'BUTTON', NULL, 3),
       (201, 197, '分类删除', 'resource:category:delete', 'BUTTON', NULL, 4),
       (202, 190, '资源标签', 'resource:tag', 'MENU', '/resource/tags', 4),
       (203, 202, '标签查询', 'resource:tag:list', 'BUTTON', NULL, 1),
       (204, 202, '标签新增', 'resource:tag:create', 'BUTTON', NULL, 2),
       (205, 202, '标签修改', 'resource:tag:update', 'BUTTON', NULL, 3),
       (206, 202, '标签删除', 'resource:tag:delete', 'BUTTON', NULL, 4),
       (207, 190, '网盘类型', 'resource:disk-type', 'MENU', '/resource/disk-types', 5),
       (208, 207, '网盘查询', 'resource:disk-type:list', 'BUTTON', NULL, 1),
       (209, 207, '网盘新增', 'resource:disk-type:create', 'BUTTON', NULL, 2),
       (210, 207, '网盘修改', 'resource:disk-type:update', 'BUTTON', NULL, 3),
       (211, 207, '网盘删除', 'resource:disk-type:delete', 'BUTTON', NULL, 4);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 190 AND 211;
