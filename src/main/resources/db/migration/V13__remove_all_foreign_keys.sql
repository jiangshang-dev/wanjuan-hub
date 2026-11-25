-- 万卷汇不依赖数据库外键。表间归属、存在性、删除顺序和账务一致性由 Service 事务、
-- 唯一键、普通索引、条件更新和必要的行锁保证。
ALTER TABLE user_profile DROP FOREIGN KEY fk_profile_user;
ALTER TABLE sys_admin_role
    DROP FOREIGN KEY fk_admin_role_admin,
    DROP FOREIGN KEY fk_admin_role_role;
ALTER TABLE sys_role_permission
    DROP FOREIGN KEY fk_role_permission_role,
    DROP FOREIGN KEY fk_role_permission_permission;

ALTER TABLE file_reference DROP FOREIGN KEY fk_reference_file;
ALTER TABLE audit_item DROP FOREIGN KEY fk_audit_item_task;
ALTER TABLE audit_manual_record DROP FOREIGN KEY fk_manual_task;
ALTER TABLE audit_outbox DROP FOREIGN KEY fk_outbox_task;

ALTER TABLE blog_article_version DROP FOREIGN KEY fk_version_article;
ALTER TABLE blog_article_tag
    DROP FOREIGN KEY fk_article_tag_article,
    DROP FOREIGN KEY fk_article_tag_tag;

ALTER TABLE forum_board
    DROP FOREIGN KEY fk_board_section,
    DROP FOREIGN KEY fk_board_parent;
ALTER TABLE forum_moderator
    DROP FOREIGN KEY fk_moderator_board,
    DROP FOREIGN KEY fk_moderator_admin;
ALTER TABLE forum_topic
    DROP FOREIGN KEY fk_topic_board,
    DROP FOREIGN KEY fk_topic_author;
ALTER TABLE forum_topic_version DROP FOREIGN KEY fk_topic_version_topic;
ALTER TABLE forum_post
    DROP FOREIGN KEY fk_post_topic,
    DROP FOREIGN KEY fk_post_board,
    DROP FOREIGN KEY fk_post_author,
    DROP FOREIGN KEY fk_post_parent,
    DROP FOREIGN KEY fk_post_reply_to;
ALTER TABLE forum_post_version DROP FOREIGN KEY fk_post_version_post;
ALTER TABLE content_comment
    DROP FOREIGN KEY fk_comment_author,
    DROP FOREIGN KEY fk_comment_parent,
    DROP FOREIGN KEY fk_comment_reply_to;
ALTER TABLE content_comment_version DROP FOREIGN KEY fk_comment_version_comment;
ALTER TABLE content_like DROP FOREIGN KEY fk_like_user;
ALTER TABLE content_favorite DROP FOREIGN KEY fk_favorite_user;
ALTER TABLE user_follow
    DROP FOREIGN KEY fk_follow_follower,
    DROP FOREIGN KEY fk_follow_followee;
ALTER TABLE content_report DROP FOREIGN KEY fk_report_reporter;
ALTER TABLE content_report_handle
    DROP FOREIGN KEY fk_report_handle_report,
    DROP FOREIGN KEY fk_report_handle_admin;

ALTER TABLE resource_info DROP FOREIGN KEY fk_resource_category;
ALTER TABLE resource_version DROP FOREIGN KEY fk_resource_version_resource;
ALTER TABLE resource_link
    DROP FOREIGN KEY fk_resource_link_resource,
    DROP FOREIGN KEY fk_resource_link_disk;
ALTER TABLE resource_tag_relation
    DROP FOREIGN KEY fk_resource_tag_resource,
    DROP FOREIGN KEY fk_resource_tag_tag;
ALTER TABLE content_resource_relation DROP FOREIGN KEY fk_content_resource_resource;
ALTER TABLE resource_entitlement DROP FOREIGN KEY fk_resource_entitlement_resource;
ALTER TABLE resource_access_record DROP FOREIGN KEY fk_resource_access_resource;
ALTER TABLE resource_invalid_report
    DROP FOREIGN KEY fk_resource_invalid_resource,
    DROP FOREIGN KEY fk_resource_invalid_link;
ALTER TABLE resource_point_transaction DROP FOREIGN KEY fk_resource_point_resource;

ALTER TABLE trade_order
    DROP FOREIGN KEY fk_trade_order_user,
    DROP FOREIGN KEY fk_trade_order_resource;
ALTER TABLE trade_order_item
    DROP FOREIGN KEY fk_trade_order_item_order,
    DROP FOREIGN KEY fk_trade_order_item_resource;
ALTER TABLE trade_payment DROP FOREIGN KEY fk_trade_payment_order;
ALTER TABLE trade_payment_notification
    DROP FOREIGN KEY fk_trade_payment_notification_order,
    DROP FOREIGN KEY fk_trade_payment_notification_payment;
ALTER TABLE trade_refund
    DROP FOREIGN KEY fk_trade_refund_order,
    DROP FOREIGN KEY fk_trade_refund_payment,
    DROP FOREIGN KEY fk_trade_refund_user,
    DROP FOREIGN KEY fk_trade_refund_reviewer;
ALTER TABLE trade_reconciliation
    DROP FOREIGN KEY fk_trade_reconciliation_order,
    DROP FOREIGN KEY fk_trade_reconciliation_payment,
    DROP FOREIGN KEY fk_trade_reconciliation_operator;

ALTER TABLE points_record DROP FOREIGN KEY fk_points_record_user;
ALTER TABLE user_sign_in DROP FOREIGN KEY fk_user_sign_in_user;
ALTER TABLE message_notification DROP FOREIGN KEY fk_message_user;
ALTER TABLE message_preference DROP FOREIGN KEY fk_message_preference_user;
