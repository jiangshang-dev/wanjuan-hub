INSERT INTO sys_permission(id, parent_id, name, code, type, path, sort_order)
VALUES (230, 0, '交易管理', 'trade', 'MENU', '/trade', 10),
       (231, 230, '订单管理', 'order', 'MENU', '/trade/orders', 1),
       (232, 231, '订单查询', 'order:list', 'BUTTON', NULL, 1),
       (233, 231, '订单详情', 'order:detail', 'BUTTON', NULL, 2),
       (234, 230, '支付流水', 'payment', 'MENU', '/trade/payments', 2),
       (235, 234, '支付流水查询', 'payment:list', 'BUTTON', NULL, 1),
       (236, 234, '手动对账', 'payment:reconcile', 'BUTTON', NULL, 2),
       (237, 230, '支付通知', 'payment:notification', 'MENU', '/trade/payment-notifications', 3),
       (238, 237, '支付通知查询', 'payment:notification:list', 'BUTTON', NULL, 1),
       (239, 230, '对账记录', 'payment:reconciliation', 'MENU', '/trade/reconciliations', 4),
       (240, 239, '对账记录查询', 'payment:reconciliation:list', 'BUTTON', NULL, 1),
       (241, 230, '退款管理', 'refund', 'MENU', '/trade/refunds', 5),
       (242, 241, '退款查询', 'refund:list', 'BUTTON', NULL, 1),
       (243, 241, '退款审核', 'refund:review', 'BUTTON', NULL, 2),
       (244, 241, '退款同步', 'refund:sync', 'BUTTON', NULL, 3),
       (245, 230, '资源授权', 'resource:entitlement', 'MENU', '/trade/entitlements', 6),
       (246, 245, '资源授权查询', 'resource:entitlement:list', 'BUTTON', NULL, 1);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 230 AND 246;
