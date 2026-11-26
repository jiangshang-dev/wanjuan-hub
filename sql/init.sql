
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `wanjuan_hub` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `wanjuan_hub`;
DROP TABLE IF EXISTS `audit_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `item_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `suggestion` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_request_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_item_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `audit_item` WRITE;
/*!40000 ALTER TABLE `audit_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_item` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `audit_manual_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_manual_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `reviewer_id` bigint unsigned NOT NULL,
  `decision` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_manual_task_created` (`task_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `audit_manual_record` WRITE;
/*!40000 ALTER TABLE `audit_manual_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_manual_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `audit_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_outbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_id` bigint unsigned NOT NULL,
  `payload` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int unsigned NOT NULL DEFAULT '0',
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lease_owner` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `last_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_message` (`message_id`),
  UNIQUE KEY `uk_outbox_task` (`task_id`),
  KEY `idx_outbox_scan` (`status`,`next_retry_at`,`lease_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `audit_outbox` WRITE;
/*!40000 ALTER TABLE `audit_outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_outbox` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `audit_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_id` bigint unsigned NOT NULL,
  `author_id` bigint unsigned DEFAULT NULL,
  `version_no` int unsigned NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `retry_count` int unsigned NOT NULL DEFAULT '0',
  `lease_owner` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_biz_version` (`biz_type`,`biz_id`,`version_no`),
  KEY `idx_audit_status_created` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `audit_task` WRITE;
/*!40000 ALTER TABLE `audit_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_task` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `auth_refresh_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_refresh_token` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `token_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `family_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `replaced_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_id` (`token_id`),
  UNIQUE KEY `uk_refresh_hash` (`token_hash`),
  KEY `idx_refresh_user` (`user_type`,`user_id`),
  KEY `idx_refresh_family` (`family_id`),
  KEY `idx_refresh_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `auth_refresh_token` WRITE;
/*!40000 ALTER TABLE `auth_refresh_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `auth_refresh_token` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `blog_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_article` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `author_id` bigint unsigned NOT NULL,
  `category_id` bigint unsigned NOT NULL,
  `original_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORIGINAL',
  `source_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `latest_version_no` int unsigned NOT NULL,
  `published_version_no` int unsigned DEFAULT NULL,
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `view_count` bigint unsigned NOT NULL DEFAULT '0',
  `like_count` bigint unsigned NOT NULL DEFAULT '0',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0',
  `favorite_count` bigint unsigned NOT NULL DEFAULT '0',
  `comment_count` bigint unsigned NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_article_author_created` (`author_id`,`create_time`),
  KEY `idx_article_public` (`deleted`,`status`,`published_version_no`,`category_id`),
  KEY `idx_article_status_updated` (`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `blog_article` WRITE;
/*!40000 ALTER TABLE `blog_article` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog_article` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `blog_article_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_article_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `article_id` bigint unsigned NOT NULL,
  `tag_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`),
  KEY `idx_article_tag_tag` (`tag_id`,`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `blog_article_tag` WRITE;
/*!40000 ALTER TABLE `blog_article_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog_article_tag` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `blog_article_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_article_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `article_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_file_id` bigint unsigned DEFAULT NULL,
  `content_file_ids` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_version` (`article_id`,`version_no`),
  KEY `idx_version_status` (`status`,`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `blog_article_version` WRITE;
/*!40000 ALTER TABLE `blog_article_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog_article_version` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `blog_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_category_name` (`name`),
  KEY `idx_blog_category_status_sort` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `blog_category` WRITE;
/*!40000 ALTER TABLE `blog_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog_category` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `blog_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `blog_tag` WRITE;
/*!40000 ALTER TABLE `blog_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `blog_tag` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `target_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `author_id` bigint unsigned NOT NULL,
  `root_id` bigint unsigned DEFAULT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `reply_to_id` bigint unsigned DEFAULT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `latest_version_no` int unsigned NOT NULL DEFAULT '1',
  `published_version_no` int unsigned DEFAULT NULL,
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reply_count` bigint unsigned NOT NULL DEFAULT '0',
  `like_count` bigint unsigned NOT NULL DEFAULT '0',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_target_root` (`target_type`,`target_id`,`root_id`,`deleted`,`status`,`id`),
  KEY `idx_comment_root_replies` (`root_id`,`deleted`,`status`,`id`),
  KEY `idx_comment_author_created` (`author_id`,`deleted`,`create_time`,`id`),
  KEY `idx_comment_parent` (`parent_id`,`deleted`,`id`),
  KEY `idx_comment_admin` (`status`,`deleted`,`create_time`,`id`),
  KEY `fk_comment_reply_to` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_comment` WRITE;
/*!40000 ALTER TABLE `content_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_comment` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_comment_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_comment_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `comment_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_file_ids` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_version` (`comment_id`,`version_no`),
  KEY `idx_comment_version_status` (`comment_id`,`status`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_comment_version` WRITE;
/*!40000 ALTER TABLE `content_comment_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_comment_version` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `target_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_favorite_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_favorite_target` (`target_type`,`target_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_favorite` WRITE;
/*!40000 ALTER TABLE `content_favorite` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_favorite` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `target_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `reaction_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LIKE' COMMENT 'LIKE点赞/DISLIKE点踩',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_like_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_like_target` (`target_type`,`target_id`,`user_id`),
  KEY `idx_reaction_target_type` (`target_type`,`target_id`,`reaction_type`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_like` WRITE;
/*!40000 ALTER TABLE `content_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_like` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint unsigned NOT NULL,
  `target_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `reason` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status_created` (`status`,`create_time`,`id`),
  KEY `idx_report_reporter_created` (`reporter_id`,`create_time`,`id`),
  KEY `idx_report_target` (`target_type`,`target_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_report` WRITE;
/*!40000 ALTER TABLE `content_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_report` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_report_handle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_report_handle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `report_id` bigint unsigned NOT NULL,
  `handler_id` bigint unsigned NOT NULL,
  `action` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_handle_report` (`report_id`),
  KEY `idx_report_handle_handler` (`handler_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_report_handle` WRITE;
/*!40000 ALTER TABLE `content_report_handle` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_report_handle` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `content_resource_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_resource_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `content_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_resource` (`content_type`,`content_id`,`resource_id`),
  KEY `idx_content_resource_resource` (`resource_id`,`content_type`,`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `content_resource_relation` WRITE;
/*!40000 ALTER TABLE `content_resource_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `content_resource_relation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `file_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_object` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` bigint unsigned NOT NULL,
  `file_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bucket_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_key` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `public_bucket` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `public_object_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `extension` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_bytes` bigint unsigned NOT NULL,
  `sha256` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `visibility` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIVATE',
  `audit_status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_bucket_key` (`bucket_name`,`object_key`),
  KEY `idx_file_sha256` (`sha256`),
  KEY `idx_file_owner_created` (`owner_id`,`create_time`),
  KEY `idx_file_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `file_object` WRITE;
/*!40000 ALTER TABLE `file_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_object` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `file_reference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_reference` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `file_id` bigint unsigned NOT NULL,
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_reference` (`file_id`,`biz_type`,`biz_id`,`version_no`),
  KEY `idx_reference_biz` (`biz_type`,`biz_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `file_reference` WRITE;
/*!40000 ALTER TABLE `file_reference` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_reference` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_board` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `section_id` bigint unsigned NOT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `post_permission` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `reply_permission` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `topic_count` bigint unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_parent_name` (`section_id`,`parent_id`,`name`),
  KEY `idx_board_public` (`status`,`section_id`,`parent_id`,`sort_order`,`id`),
  KEY `idx_board_parent` (`parent_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_board` WRITE;
/*!40000 ALTER TABLE `forum_board` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_board` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_moderator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_moderator` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `board_id` bigint unsigned NOT NULL,
  `admin_id` bigint unsigned NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moderator_board_admin` (`board_id`,`admin_id`),
  KEY `idx_moderator_admin_board` (`admin_id`,`status`,`board_id`),
  KEY `idx_moderator_board_status` (`board_id`,`status`,`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_moderator` WRITE;
/*!40000 ALTER TABLE `forum_moderator` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_moderator` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `topic_id` bigint unsigned NOT NULL,
  `board_id` bigint unsigned NOT NULL,
  `author_id` bigint unsigned NOT NULL,
  `root_id` bigint unsigned DEFAULT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `reply_to_id` bigint unsigned DEFAULT NULL,
  `floor_no` int unsigned NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `latest_version_no` int unsigned NOT NULL DEFAULT '1',
  `published_version_no` int unsigned DEFAULT NULL,
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `like_count` bigint unsigned NOT NULL DEFAULT '0',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_topic_floor` (`topic_id`,`floor_no`),
  KEY `idx_post_topic_public` (`topic_id`,`deleted`,`status`,`floor_no`,`id`),
  KEY `idx_post_topic_author` (`topic_id`,`author_id`,`deleted`,`floor_no`),
  KEY `idx_post_author_created` (`author_id`,`deleted`,`create_time`,`id`),
  KEY `idx_post_parent` (`parent_id`,`deleted`,`id`),
  KEY `idx_post_root` (`root_id`,`deleted`,`floor_no`),
  KEY `fk_post_board` (`board_id`),
  KEY `fk_post_reply_to` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_post` WRITE;
/*!40000 ALTER TABLE `forum_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_post` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_post_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_post_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_file_ids` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_version` (`post_id`,`version_no`),
  KEY `idx_post_version_status` (`post_id`,`status`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_post_version` WRITE;
/*!40000 ALTER TABLE `forum_post_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_post_version` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_section` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_section_name` (`name`),
  KEY `idx_section_status_sort` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_section` WRITE;
/*!40000 ALTER TABLE `forum_section` DISABLE KEYS */;
INSERT INTO `forum_section` VALUES (1,'综合交流',0,1,'2026-11-25 12:19:12','2026-11-25 12:19:12');
/*!40000 ALTER TABLE `forum_section` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_topic` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `board_id` bigint unsigned NOT NULL,
  `author_id` bigint unsigned NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `latest_version_no` int unsigned NOT NULL DEFAULT '1',
  `published_version_no` int unsigned DEFAULT NULL,
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locked` tinyint NOT NULL DEFAULT '0',
  `topped` tinyint NOT NULL DEFAULT '0',
  `essence` tinyint NOT NULL DEFAULT '0',
  `post_count` bigint unsigned NOT NULL DEFAULT '0',
  `comment_count` bigint unsigned NOT NULL DEFAULT '0',
  `like_count` bigint unsigned NOT NULL DEFAULT '0',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0',
  `favorite_count` bigint unsigned NOT NULL DEFAULT '0',
  `view_count` bigint unsigned NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_topic_public` (`deleted`,`status`,`board_id`,`topped`,`id`),
  KEY `idx_topic_board_created` (`board_id`,`deleted`,`create_time`,`id`),
  KEY `idx_topic_author_created` (`author_id`,`deleted`,`create_time`,`id`),
  KEY `idx_topic_status_updated` (`status`,`deleted`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_topic` WRITE;
/*!40000 ALTER TABLE `forum_topic` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_topic` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `forum_topic_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_topic_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `topic_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_file_ids` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_version` (`topic_id`,`version_no`),
  KEY `idx_topic_version_status` (`topic_id`,`status`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `forum_topic_version` WRITE;
/*!40000 ALTER TABLE `forum_topic_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_topic_version` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint unsigned DEFAULT NULL,
  `account` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `login_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知',
  `success` tinyint NOT NULL,
  `message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_user` (`user_type`,`user_id`,`create_time`),
  KEY `idx_login_account` (`account`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `login_log` WRITE;
/*!40000 ALTER TABLE `login_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `membership_commission_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_commission_config` (
  `id` bigint unsigned NOT NULL,
  `points_unit_price` decimal(18,4) NOT NULL DEFAULT '0.0100',
  `member_fixed_amount` decimal(18,2) NOT NULL DEFAULT '1.00',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `membership_commission_config` WRITE;
/*!40000 ALTER TABLE `membership_commission_config` DISABLE KEYS */;
INSERT INTO `membership_commission_config` VALUES (1,0.0100,1.00,'2026-11-25 12:19:12','2026-11-25 12:19:12');
/*!40000 ALTER TABLE `membership_commission_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `membership_daily_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_daily_usage` (
  `user_id` bigint unsigned NOT NULL,
  `claim_date` date NOT NULL,
  `used_count` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`claim_date`),
  KEY `idx_membership_daily_date` (`claim_date`,`used_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `membership_daily_usage` WRITE;
/*!40000 ALTER TABLE `membership_daily_usage` DISABLE KEYS */;
/*!40000 ALTER TABLE `membership_daily_usage` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `membership_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `duration_days` int unsigned NOT NULL,
  `price` decimal(18,2) NOT NULL,
  `daily_resource_limit` int unsigned NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_membership_plan_public` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `membership_plan` WRITE;
/*!40000 ALTER TABLE `membership_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `membership_plan` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `membership_resource_claim`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_resource_claim` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `claim_date` date NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_claim_user_resource` (`user_id`,`resource_id`),
  KEY `idx_membership_claim_user_date` (`user_id`,`claim_date`,`id`),
  KEY `idx_membership_claim_resource` (`resource_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `membership_resource_claim` WRITE;
/*!40000 ALTER TABLE `membership_resource_claim` DISABLE KEYS */;
/*!40000 ALTER TABLE `membership_resource_claim` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `biz_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dedup_key` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_flag` tinyint NOT NULL DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user_dedup` (`user_id`,`dedup_key`),
  KEY `idx_message_user_read_created` (`user_id`,`read_flag`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_notification` WRITE;
/*!40000 ALTER TABLE `message_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_notification` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_preference` (
  `user_id` bigint unsigned NOT NULL,
  `audit_enabled` tinyint NOT NULL DEFAULT '1',
  `payment_enabled` tinyint NOT NULL DEFAULT '1',
  `system_enabled` tinyint NOT NULL DEFAULT '1',
  `interaction_enabled` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_preference` WRITE;
/*!40000 ALTER TABLE `message_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_preference` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `mq_consume_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mq_consume_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `consumer_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consume_message` (`consumer_name`,`message_id`),
  KEY `idx_consume_status_created` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `mq_consume_log` WRITE;
/*!40000 ALTER TABLE `mq_consume_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `mq_consume_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_advertisement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_advertisement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_ad_public` (`position`,`status`,`start_time`,`end_time`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_advertisement` WRITE;
/*!40000 ALTER TABLE `operation_advertisement` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_advertisement` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_background_music`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_background_music` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '0关闭，1开启',
  `audio_url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `play_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO自动播放，MANUAL手动播放',
  `player_visible` tinyint NOT NULL DEFAULT '1' COMMENT '0隐藏播放器，1显示播放器',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_background_music` WRITE;
/*!40000 ALTER TABLE `operation_background_music` DISABLE KEYS */;
INSERT INTO `operation_background_music` VALUES (1,0,'','AUTO',1,'2026-11-25 12:19:12','2026-11-25 12:19:12');
/*!40000 ALTER TABLE `operation_background_music` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待回复，REPLIED已回复，CLOSED已关闭',
  `admin_reply` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reply_admin_id` bigint unsigned DEFAULT NULL,
  `reply_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_feedback_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_operation_feedback_status_created` (`status`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_feedback` WRITE;
/*!40000 ALTER TABLE `operation_feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_feedback` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_friend_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_friend_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `logo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_friend_link_name` (`name`),
  KEY `idx_operation_friend_link_status_sort` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_friend_link` WRITE;
/*!40000 ALTER TABLE `operation_friend_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_friend_link` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_gray_day`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_gray_day` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `gray_date` date NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0停用，1启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_gray_day_date` (`gray_date`),
  KEY `idx_operation_gray_day_status_date` (`status`,`gray_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_gray_day` WRITE;
/*!40000 ALTER TABLE `operation_gray_day` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_gray_day` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `operator_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ANONYMOUS',
  `operator_id` bigint unsigned DEFAULT NULL,
  `operator_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'anonymous',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知',
  `success` tinyint NOT NULL,
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cost_ms` bigint NOT NULL DEFAULT '0',
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_operator` (`operator_type`,`operator_id`,`create_time`),
  KEY `idx_operation_module` (`module`,`create_time`),
  KEY `idx_operation_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_notice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `publish_start` datetime DEFAULT NULL,
  `publish_end` datetime DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_notice_public` (`status`,`publish_start`,`publish_end`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_notice` WRITE;
/*!40000 ALTER TABLE `operation_notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_notice` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_recommend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_recommend` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `content_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_id` bigint unsigned NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_recommend_content` (`content_type`,`content_id`),
  KEY `idx_operation_recommend_status_sort` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_recommend` WRITE;
/*!40000 ALTER TABLE `operation_recommend` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_recommend` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `points_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_account` (
  `user_id` bigint unsigned NOT NULL,
  `balance` int unsigned NOT NULL DEFAULT '0',
  `total_points` bigint unsigned NOT NULL DEFAULT '0',
  `experience` bigint unsigned NOT NULL DEFAULT '0',
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_points_account_experience` (`experience`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `points_account` WRITE;
/*!40000 ALTER TABLE `points_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `points_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `points_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `change_amount` int NOT NULL,
  `balance_after` int unsigned NOT NULL,
  `experience_change` int NOT NULL DEFAULT '0',
  `experience_after` bigint unsigned NOT NULL DEFAULT '0',
  `biz_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_id` bigint unsigned DEFAULT NULL,
  `idempotency_key` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_points_record_idempotency` (`idempotency_key`),
  KEY `idx_points_record_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_points_record_biz` (`user_id`,`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `points_record` WRITE;
/*!40000 ALTER TABLE `points_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `points_record` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_points_record_immutable_update` BEFORE UPDATE ON `points_record` FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'points_record is immutable';
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_points_record_immutable_delete` BEFORE DELETE ON `points_record` FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'points_record is immutable';
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
DROP TABLE IF EXISTS `points_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `points` int NOT NULL,
  `experience` int NOT NULL DEFAULT '0',
  `daily_limit` int NOT NULL DEFAULT '0' COMMENT '0表示不限制',
  `status` tinyint NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_points_rule_code` (`code`),
  KEY `idx_points_rule_status` (`status`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `points_rule` WRITE;
/*!40000 ALTER TABLE `points_rule` DISABLE KEYS */;
INSERT INTO `points_rule` VALUES (1,'SIGN_IN','每日签到',10,10,10,1,'每日首次签到奖励','2026-11-25 12:19:11','2026-11-25 12:19:11'),(2,'BLOG_ARTICLE','博客审核通过',20,20,100,1,'按审核任务幂等发放','2026-11-25 12:19:11','2026-11-25 12:19:11'),(3,'RESOURCE','资源审核通过',30,30,150,1,'按审核任务幂等发放','2026-11-25 12:19:11','2026-11-25 12:19:11'),(4,'FORUM_TOPIC','论坛主题审核通过',15,15,75,1,'按审核任务幂等发放','2026-11-25 12:19:11','2026-11-25 12:19:11');
/*!40000 ALTER TABLE `points_rule` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_access_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_access_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned DEFAULT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `target_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_access_user` (`user_id`,`resource_id`,`create_time`),
  KEY `idx_resource_access_resource` (`resource_id`,`target_type`,`target_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_access_record` WRITE;
/*!40000 ALTER TABLE `resource_access_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_access_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_category_name` (`name`),
  KEY `idx_resource_category_status_sort` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_category` WRITE;
/*!40000 ALTER TABLE `resource_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_category` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_disk_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_disk_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `allowed_hosts` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_disk_code` (`code`),
  KEY `idx_resource_disk_status_sort` (`status`,`sort_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_disk_type` WRITE;
/*!40000 ALTER TABLE `resource_disk_type` DISABLE KEYS */;
INSERT INTO `resource_disk_type` VALUES (1,'BAIDU','百度网盘','pan.baidu.com',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(2,'QUARK','夸克网盘','pan.quark.cn',2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(3,'ALIPAN','阿里云盘','www.alipan.com,www.aliyundrive.com',3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(4,'PAN123','123云盘','www.123pan.com,www.123684.com',4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(5,'CT189','天翼云盘','cloud.189.cn',5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11');
/*!40000 ALTER TABLE `resource_disk_type` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_entitlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_entitlement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `source_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_order_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `points_amount` int unsigned DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `revoke_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `granted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_entitlement_user_resource` (`user_id`,`resource_id`),
  UNIQUE KEY `uk_resource_entitlement_order` (`source_type`,`source_order_no`),
  KEY `idx_resource_entitlement_resource` (`resource_id`,`status`,`user_id`),
  KEY `idx_resource_entitlement_user_created` (`user_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_entitlement` WRITE;
/*!40000 ALTER TABLE `resource_entitlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_entitlement` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `author_id` bigint unsigned NOT NULL,
  `category_id` bigint unsigned NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `latest_version_no` int unsigned NOT NULL,
  `published_version_no` int unsigned DEFAULT NULL,
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `online` tinyint NOT NULL DEFAULT '0',
  `invalid` tinyint NOT NULL DEFAULT '0',
  `view_count` bigint unsigned NOT NULL DEFAULT '0',
  `comment_count` bigint unsigned NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_author_created` (`author_id`,`deleted`,`create_time`),
  KEY `idx_resource_public` (`deleted`,`online`,`invalid`,`published_version_no`,`category_id`),
  KEY `idx_resource_status_updated` (`status`,`deleted`,`update_time`),
  KEY `fk_resource_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_info` WRITE;
/*!40000 ALTER TABLE `resource_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_info` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_invalid_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_invalid_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint unsigned NOT NULL,
  `link_id` bigint unsigned DEFAULT NULL,
  `reporter_id` bigint unsigned NOT NULL,
  `reason` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `handler_id` bigint unsigned DEFAULT NULL,
  `handle_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handled_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_invalid_status` (`status`,`create_time`,`id`),
  KEY `idx_resource_invalid_resource` (`resource_id`,`status`,`create_time`),
  KEY `idx_resource_invalid_reporter` (`reporter_id`,`create_time`),
  KEY `fk_resource_invalid_link` (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_invalid_report` WRITE;
/*!40000 ALTER TABLE `resource_invalid_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_invalid_report` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `disk_type_id` bigint unsigned NOT NULL,
  `url_ciphertext` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `extraction_code_ciphertext` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_link_version` (`resource_id`,`version_no`,`status`,`sort_order`,`id`),
  KEY `fk_resource_link_disk` (`disk_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_link` WRITE;
/*!40000 ALTER TABLE `resource_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_link` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_point_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_point_account` (
  `user_id` bigint unsigned NOT NULL,
  `balance` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_resource_point_balance` (`balance`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_point_account` WRITE;
/*!40000 ALTER TABLE `resource_point_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_point_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_point_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_point_transaction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `change_amount` int NOT NULL,
  `balance_after` int unsigned NOT NULL,
  `biz_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_point_biz` (`user_id`,`biz_type`,`resource_id`),
  KEY `idx_resource_point_created` (`user_id`,`create_time`,`id`),
  KEY `fk_resource_point_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_point_transaction` WRITE;
/*!40000 ALTER TABLE `resource_point_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_point_transaction` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_tag` WRITE;
/*!40000 ALTER TABLE `resource_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_tag` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_tag_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_tag_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint unsigned NOT NULL,
  `tag_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_tag_relation` (`resource_id`,`tag_id`),
  KEY `idx_resource_tag_relation_tag` (`tag_id`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_tag_relation` WRITE;
/*!40000 ALTER TABLE `resource_tag_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_tag_relation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `resource_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint unsigned NOT NULL,
  `version_no` int unsigned NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_file_id` bigint unsigned DEFAULT NULL,
  `attachment_file_ids` varchar(4000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]',
  `access_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `points_price` int unsigned DEFAULT NULL,
  `price` decimal(18,2) DEFAULT NULL,
  `copyright_notice` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_version` (`resource_id`,`version_no`),
  KEY `idx_resource_version_status` (`resource_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `resource_version` WRITE;
/*!40000 ALTER TABLE `resource_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_version` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `search_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_document` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `content_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_id` bigint unsigned NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_document_public` (`content_type`,`content_id`),
  FULLTEXT KEY `ft_search_document_ngram` (`title`,`summary`,`content`) /*!50100 WITH PARSER `ngram` */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `search_document` WRITE;
/*!40000 ALTER TABLE `search_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_document` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `search_hot_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_hot_keyword` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `keyword` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `heat` bigint unsigned NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_hot_keyword` (`keyword`),
  KEY `idx_search_hot_rank` (`status`,`sort_order`,`heat`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `search_hot_keyword` WRITE;
/*!40000 ALTER TABLE `search_hot_keyword` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_hot_keyword` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `search_inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_inbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_inbox_message` (`message_id`),
  KEY `idx_search_inbox_status_created` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `search_inbox` WRITE;
/*!40000 ALTER TABLE `search_inbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_inbox` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `search_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_outbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_id` bigint unsigned NOT NULL,
  `event_version` bigint unsigned NOT NULL,
  `payload` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int unsigned NOT NULL DEFAULT '0',
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lease_owner` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `last_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_outbox_message` (`message_id`),
  KEY `idx_search_outbox_content` (`content_type`,`content_id`,`id`),
  KEY `idx_search_outbox_scan` (`status`,`next_retry_at`,`lease_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `search_outbox` WRITE;
/*!40000 ALTER TABLE `search_outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_outbox` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sign_in_streak_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sign_in_streak_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `streak_day` int unsigned NOT NULL COMMENT '连续签到第N天',
  `points` int unsigned NOT NULL DEFAULT '0' COMMENT '该档总奖励积分',
  `experience` int unsigned NOT NULL DEFAULT '0' COMMENT '该档总奖励经验',
  `status` tinyint NOT NULL DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sign_in_streak_day` (`streak_day`),
  KEY `idx_sign_in_streak_status_day` (`status`,`streak_day`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sign_in_streak_rule` WRITE;
/*!40000 ALTER TABLE `sign_in_streak_rule` DISABLE KEYS */;
INSERT INTO `sign_in_streak_rule` VALUES (1,1,10,10,1,'连续签到第1天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(2,2,12,12,1,'连续签到第2天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(3,3,15,15,1,'连续签到第3天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(4,4,20,20,1,'连续签到第4天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(5,5,25,25,1,'连续签到第5天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(6,6,30,30,1,'连续签到第6天','2026-11-25 12:19:12','2026-11-25 12:19:12'),(7,7,50,50,1,'连续签到第7天及以后沿用该档','2026-11-25 12:19:12','2026-11-25 12:19:12');
/*!40000 ALTER TABLE `sign_in_streak_rule` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `statistics_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistics_daily` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `new_users` bigint unsigned NOT NULL DEFAULT '0',
  `new_contents` bigint unsigned NOT NULL DEFAULT '0',
  `audit_count` bigint unsigned NOT NULL DEFAULT '0',
  `order_count` bigint unsigned NOT NULL DEFAULT '0',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statistics_daily_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `statistics_daily` WRITE;
/*!40000 ALTER TABLE `statistics_daily` DISABLE KEYS */;
/*!40000 ALTER TABLE `statistics_daily` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_admin` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `token_version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  UNIQUE KEY `uk_admin_email` (`email`),
  KEY `idx_admin_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_admin` WRITE;
/*!40000 ALTER TABLE `sys_admin` DISABLE KEYS */;
INSERT INTO `sys_admin` (`id`,`username`,`nickname`,`email`,`password`,`status`,`token_version`) VALUES (1,'admin','系统管理员',NULL,'$2b$12$lJA9ahmb2pCgbHQcUhAwOeS5ODTnvdI3MlPXojRHYU7/8lTPCadlO',1,0);
/*!40000 ALTER TABLE `sys_admin` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_admin_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_admin_role` (
  `admin_id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`admin_id`,`role_id`),
  KEY `idx_admin_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_admin_role` WRITE;
/*!40000 ALTER TABLE `sys_admin_role` DISABLE KEYS */;
INSERT INTO `sys_admin_role` (`admin_id`,`role_id`) VALUES (1,1);
/*!40000 ALTER TABLE `sys_admin_role` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `public_flag` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`),
  KEY `idx_sys_config_public` (`public_flag`,`status`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'site.name','万卷汇','网站名称',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(2,'site.userAgreement','请遵守法律法规与社区规范，仅分享有权发布的内容。','用户协议',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(3,'site.privacyPolicy','我们仅在提供社区服务所需范围内处理账号与使用数据。','隐私政策',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(4,'site.icp','','ICP备案号',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(5,'site.copyright','万卷汇','版权信息',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` bigint unsigned NOT NULL DEFAULT '0',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BUTTON',
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_parent` (`parent_id`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=512 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,0,'系统管理','system','MENU','/system',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(2,1,'管理员管理','system:admin','MENU','/system/admins',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(3,2,'管理员查询','system:admin:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(4,2,'管理员新增','system:admin:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(5,2,'管理员修改','system:admin:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(6,2,'管理员状态','system:admin:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(7,2,'管理员删除','system:admin:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(8,2,'分配角色','system:admin:assign-role','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(9,1,'角色管理','system:role','MENU','/system/roles',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(10,9,'角色查询','system:role:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(11,9,'角色新增','system:role:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(12,9,'角色修改','system:role:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(13,9,'角色状态','system:role:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(14,9,'角色删除','system:role:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(15,9,'分配权限','system:role:assign-permission','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(16,1,'权限管理','system:permission','MENU','/system/permissions',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(17,16,'权限查询','system:permission:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(18,16,'权限新增','system:permission:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(19,16,'权限修改','system:permission:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(20,16,'权限状态','system:permission:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(21,16,'权限删除','system:permission:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(22,1,'用户管理','system:user','MENU','/system/users',NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(23,22,'用户查询','system:user:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(24,22,'用户状态','system:user:status','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(25,22,'用户删除','system:user:delete','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(26,1,'操作日志','system:operation-log','MENU','/system/operation-logs',NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(27,26,'日志查询','system:operation-log:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(28,22,'用户新增','system:user:create','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(29,22,'用户修改','system:user:update','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(100,0,'内容管理','content','MENU','/content',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(101,100,'博客管理','blog:article','MENU','/content/blogs',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(102,101,'博客查询','blog:article:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(103,101,'博客详情','blog:article:detail','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(104,101,'博客状态','blog:article:status','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(105,100,'博客分类','blog:category','MENU','/content/blog-categories',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(106,105,'分类查询','blog:category:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(107,105,'分类新增','blog:category:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(108,105,'分类修改','blog:category:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(109,105,'分类删除','blog:category:delete','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(120,0,'审核中心','audit:task','MENU','/audits',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(121,120,'审核查询','audit:task:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(122,120,'审核详情','audit:task:detail','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(123,120,'审核重试','audit:task:retry','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(124,120,'人工审核','audit:task:review','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(130,0,'论坛管理','forum','MENU','/forum',NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(131,130,'板块与版主','forum:board','MENU','/forum/boards',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(132,131,'板块查询','forum:board:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(133,131,'板块新增','forum:board:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(134,131,'板块修改','forum:board:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(135,131,'板块删除','forum:board:delete','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(136,131,'版主查询','forum:moderator:list','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(137,131,'版主新增','forum:moderator:create','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(138,131,'版主修改','forum:moderator:update','BUTTON',NULL,NULL,7,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(139,131,'版主删除','forum:moderator:delete','BUTTON',NULL,NULL,8,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(140,130,'主题管理','forum:topic','MENU','/forum/topics',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(141,140,'主题查询','forum:topic:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(142,140,'主题新增','forum:topic:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(143,140,'主题详情','forum:topic:detail','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(144,140,'主题修改','forum:topic:update','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(145,140,'主题删除','forum:topic:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(146,140,'主题状态','forum:topic:status','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(147,140,'主题运营','forum:topic:moderate','BUTTON',NULL,NULL,7,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(150,130,'跟帖管理','forum:post','MENU','/forum/posts',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(151,150,'跟帖查询','forum:post:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(152,150,'跟帖新增','forum:post:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(153,150,'跟帖详情','forum:post:detail','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(154,150,'跟帖修改','forum:post:update','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(155,150,'跟帖删除','forum:post:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(156,150,'跟帖状态','forum:post:status','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(170,0,'评论管理','comment:manage','MENU','/comments',NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(171,170,'评论查询','comment:manage:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(172,170,'评论状态','comment:manage:status','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(180,0,'举报管理','report:manage','MENU','/reports',NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(181,180,'举报查询','report:manage:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(182,180,'举报处理','report:manage:handle','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(190,0,'资源管理','resource','MENU','/resource',NULL,7,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(191,190,'资源列表','resource:manage','MENU','/resource/list',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(192,191,'资源查询','resource:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(193,191,'资源状态','resource:status','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(194,190,'失效举报','resource:report','MENU','/resource/reports',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(195,194,'举报查询','resource:report:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(196,194,'举报处理','resource:report:handle','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(197,190,'资源分类','resource:category','MENU','/resource/categories',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(198,197,'分类查询','resource:category:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(199,197,'分类新增','resource:category:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(200,197,'分类修改','resource:category:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(201,197,'分类删除','resource:category:delete','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(202,190,'资源标签','resource:tag','MENU','/resource/tags',NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(203,202,'标签查询','resource:tag:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(204,202,'标签新增','resource:tag:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(205,202,'标签修改','resource:tag:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(206,202,'标签删除','resource:tag:delete','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(207,190,'网盘类型','resource:disk-type','MENU','/resource/disk-types',NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(208,207,'网盘查询','resource:disk-type:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(209,207,'网盘新增','resource:disk-type:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(210,207,'网盘修改','resource:disk-type:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(211,207,'网盘删除','resource:disk-type:delete','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(230,0,'交易管理','trade','MENU','/trade',NULL,10,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(231,230,'订单管理','order','MENU','/trade/orders',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(232,231,'订单查询','order:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(233,231,'订单详情','order:detail','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(234,230,'支付流水','payment','MENU','/trade/payments',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(235,234,'支付流水查询','payment:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(236,234,'手动对账','payment:reconcile','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(237,230,'支付通知','payment:notification','MENU','/trade/payment-notifications',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(238,237,'支付通知查询','payment:notification:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(239,230,'对账记录','payment:reconciliation','MENU','/trade/reconciliations',NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(240,239,'对账记录查询','payment:reconciliation:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(241,230,'退款管理','refund','MENU','/trade/refunds',NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(242,241,'退款查询','refund:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(243,241,'退款审核','refund:review','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(244,241,'退款同步','refund:sync','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(245,230,'资源授权','resource:entitlement','MENU','/trade/entitlements',NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(246,245,'资源授权查询','resource:entitlement:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(300,0,'运营管理','operation','MENU','/operation',NULL,8,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(301,300,'公告管理','operation:announcement','MENU','/operation/announcements',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(302,301,'公告查询','operation:announcement:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(303,301,'公告新增','operation:announcement:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(304,301,'公告修改','operation:announcement:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(305,301,'公告启停','operation:announcement:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(306,301,'公告删除','operation:announcement:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(310,300,'推荐位管理','operation:recommendation','MENU','/operation/recommendations',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(311,310,'推荐位查询','operation:recommendation:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(312,310,'推荐位新增','operation:recommendation:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(313,310,'推荐位修改','operation:recommendation:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(314,310,'推荐位启停','operation:recommendation:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(315,310,'推荐位删除','operation:recommendation:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(320,300,'广告位管理','operation:ad','MENU','/operation/ads',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(321,320,'广告位查询','operation:ad:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(322,320,'广告位新增','operation:ad:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(323,320,'广告位修改','operation:ad:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(324,320,'广告位启停','operation:ad:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(325,320,'广告位删除','operation:ad:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(330,300,'友情链接','operation:link','MENU','/operation/links',NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(331,330,'链接查询','operation:link:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(332,330,'链接新增','operation:link:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(333,330,'链接修改','operation:link:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(334,330,'链接启停','operation:link:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(335,330,'链接删除','operation:link:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(340,300,'热门搜索','operation:hot-search','MENU','/operation/hot-searches',NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(341,340,'热词查询','operation:hot-search:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(342,340,'热词新增','operation:hot-search:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(343,340,'热词修改','operation:hot-search:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(344,340,'热词启停','operation:hot-search:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(345,340,'热词删除','operation:hot-search:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(350,300,'网站参数','operation:config','MENU','/operation/configs',NULL,6,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(351,350,'参数查询','operation:config:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(352,350,'参数新增','operation:config:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(353,350,'参数修改','operation:config:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(354,350,'参数启停','operation:config:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(355,350,'参数删除','operation:config:delete','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(360,0,'统计中心','statistics','MENU','/statistics',NULL,9,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(361,360,'统计工作台','statistics:dashboard','MENU','/statistics/dashboard',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(362,361,'工作台查询','statistics:dashboard:view','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(363,361,'每日汇总重算','statistics:dashboard:refresh','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(370,0,'积分管理','points','MENU','/points',NULL,10,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(371,370,'积分规则','points:rule','MENU','/points/rules',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(372,371,'规则查询','points:rule:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(373,371,'规则新增','points:rule:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(374,371,'规则修改','points:rule:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(375,370,'等级配置','points:level','MENU','/points/levels',NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(376,375,'等级查询','points:level:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(377,375,'等级新增','points:level:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(378,375,'等级修改','points:level:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(379,370,'积分调整与流水','points:adjustment','MENU','/points/adjustments',NULL,3,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(380,379,'流水查询','points:adjustment:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(381,379,'积分调整','points:adjustment:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(390,0,'消息管理','message','MENU','/messages',NULL,11,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(391,390,'系统消息','message:system','MENU','/messages/system',NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(392,391,'发送系统消息','message:system:send','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(393,371,'连续签到档位查询','points:sign-in-rule:list','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(394,371,'连续签到档位新增','points:sign-in-rule:create','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(395,371,'连续签到档位修改','points:sign-in-rule:update','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(396,371,'连续签到档位删除','points:sign-in-rule:delete','BUTTON',NULL,NULL,6,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(400,230,'会员套餐','membership:plan','MENU','/trade/membership-plans',NULL,7,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(401,400,'会员套餐查询','membership:plan:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(402,400,'会员套餐新增','membership:plan:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(403,400,'会员套餐修改','membership:plan:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(404,400,'返佣配置查询','membership:commission:detail','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(405,400,'返佣配置修改','membership:commission:update','BUTTON',NULL,NULL,5,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(406,230,'提现管理','wallet:withdrawal','MENU','/trade/withdrawals',NULL,8,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(407,406,'提现单查询','wallet:withdrawal:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(408,406,'人工确认提现','wallet:withdrawal:handle','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(500,300,'灰色日管理','operation:gray-day','MENU','/operation/gray-days',NULL,7,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(501,500,'灰色日查询','operation:gray-day:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(502,500,'灰色日新增','operation:gray-day:create','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(503,500,'灰色日修改','operation:gray-day:update','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(504,500,'灰色日启停','operation:gray-day:status','BUTTON',NULL,NULL,4,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(505,300,'背景音乐','operation:music','MENU','/operation/background-music',NULL,8,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(506,505,'音乐配置查询','operation:music:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(507,505,'音乐配置修改','operation:music:update','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(508,300,'意见反馈','operation:feedback','MENU','/operation/feedbacks',NULL,9,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(509,508,'反馈查询','operation:feedback:list','BUTTON',NULL,NULL,1,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(510,508,'反馈回复','operation:feedback:reply','BUTTON',NULL,NULL,2,1,'2026-11-25 12:19:12','2026-11-25 12:19:12'),(511,508,'反馈关闭','operation:feedback:close','BUTTON',NULL,NULL,3,1,'2026-11-25 12:19:12','2026-11-25 12:19:12');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`),
  KEY `idx_role_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'超级管理员','SUPER_ADMIN','拥有全部系统权限',1,'2026-11-25 12:19:11','2026-11-25 12:19:11');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `role_id` bigint unsigned NOT NULL,
  `permission_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (1,1,'2026-11-25 12:19:11'),(1,2,'2026-11-25 12:19:11'),(1,3,'2026-11-25 12:19:11'),(1,4,'2026-11-25 12:19:11'),(1,5,'2026-11-25 12:19:11'),(1,6,'2026-11-25 12:19:11'),(1,7,'2026-11-25 12:19:11'),(1,8,'2026-11-25 12:19:11'),(1,9,'2026-11-25 12:19:11'),(1,10,'2026-11-25 12:19:11'),(1,11,'2026-11-25 12:19:11'),(1,12,'2026-11-25 12:19:11'),(1,13,'2026-11-25 12:19:11'),(1,14,'2026-11-25 12:19:11'),(1,15,'2026-11-25 12:19:11'),(1,16,'2026-11-25 12:19:11'),(1,17,'2026-11-25 12:19:11'),(1,18,'2026-11-25 12:19:11'),(1,19,'2026-11-25 12:19:11'),(1,20,'2026-11-25 12:19:11'),(1,21,'2026-11-25 12:19:11'),(1,22,'2026-11-25 12:19:11'),(1,23,'2026-11-25 12:19:11'),(1,24,'2026-11-25 12:19:11'),(1,25,'2026-11-25 12:19:11'),(1,26,'2026-11-25 12:19:11'),(1,27,'2026-11-25 12:19:11'),(1,28,'2026-11-25 12:19:11'),(1,29,'2026-11-25 12:19:11'),(1,100,'2026-11-25 12:19:11'),(1,101,'2026-11-25 12:19:11'),(1,102,'2026-11-25 12:19:11'),(1,103,'2026-11-25 12:19:11'),(1,104,'2026-11-25 12:19:11'),(1,105,'2026-11-25 12:19:11'),(1,106,'2026-11-25 12:19:11'),(1,107,'2026-11-25 12:19:11'),(1,108,'2026-11-25 12:19:11'),(1,109,'2026-11-25 12:19:11'),(1,120,'2026-11-25 12:19:11'),(1,121,'2026-11-25 12:19:11'),(1,122,'2026-11-25 12:19:11'),(1,123,'2026-11-25 12:19:11'),(1,124,'2026-11-25 12:19:11'),(1,130,'2026-11-25 12:19:11'),(1,131,'2026-11-25 12:19:11'),(1,132,'2026-11-25 12:19:11'),(1,133,'2026-11-25 12:19:11'),(1,134,'2026-11-25 12:19:11'),(1,135,'2026-11-25 12:19:11'),(1,136,'2026-11-25 12:19:11'),(1,137,'2026-11-25 12:19:11'),(1,138,'2026-11-25 12:19:11'),(1,139,'2026-11-25 12:19:11'),(1,140,'2026-11-25 12:19:11'),(1,141,'2026-11-25 12:19:11'),(1,142,'2026-11-25 12:19:11'),(1,143,'2026-11-25 12:19:11'),(1,144,'2026-11-25 12:19:11'),(1,145,'2026-11-25 12:19:11'),(1,146,'2026-11-25 12:19:11'),(1,147,'2026-11-25 12:19:11'),(1,150,'2026-11-25 12:19:11'),(1,151,'2026-11-25 12:19:11'),(1,152,'2026-11-25 12:19:11'),(1,153,'2026-11-25 12:19:11'),(1,154,'2026-11-25 12:19:11'),(1,155,'2026-11-25 12:19:11'),(1,156,'2026-11-25 12:19:11'),(1,170,'2026-11-25 12:19:11'),(1,171,'2026-11-25 12:19:11'),(1,172,'2026-11-25 12:19:11'),(1,180,'2026-11-25 12:19:11'),(1,181,'2026-11-25 12:19:11'),(1,182,'2026-11-25 12:19:11'),(1,190,'2026-11-25 12:19:11'),(1,191,'2026-11-25 12:19:11'),(1,192,'2026-11-25 12:19:11'),(1,193,'2026-11-25 12:19:11'),(1,194,'2026-11-25 12:19:11'),(1,195,'2026-11-25 12:19:11'),(1,196,'2026-11-25 12:19:11'),(1,197,'2026-11-25 12:19:11'),(1,198,'2026-11-25 12:19:11'),(1,199,'2026-11-25 12:19:11'),(1,200,'2026-11-25 12:19:11'),(1,201,'2026-11-25 12:19:11'),(1,202,'2026-11-25 12:19:11'),(1,203,'2026-11-25 12:19:11'),(1,204,'2026-11-25 12:19:11'),(1,205,'2026-11-25 12:19:11'),(1,206,'2026-11-25 12:19:11'),(1,207,'2026-11-25 12:19:11'),(1,208,'2026-11-25 12:19:11'),(1,209,'2026-11-25 12:19:11'),(1,210,'2026-11-25 12:19:11'),(1,211,'2026-11-25 12:19:11'),(1,230,'2026-11-25 12:19:11'),(1,231,'2026-11-25 12:19:11'),(1,232,'2026-11-25 12:19:11'),(1,233,'2026-11-25 12:19:11'),(1,234,'2026-11-25 12:19:11'),(1,235,'2026-11-25 12:19:11'),(1,236,'2026-11-25 12:19:11'),(1,237,'2026-11-25 12:19:11'),(1,238,'2026-11-25 12:19:11'),(1,239,'2026-11-25 12:19:11'),(1,240,'2026-11-25 12:19:11'),(1,241,'2026-11-25 12:19:11'),(1,242,'2026-11-25 12:19:11'),(1,243,'2026-11-25 12:19:11'),(1,244,'2026-11-25 12:19:11'),(1,245,'2026-11-25 12:19:11'),(1,246,'2026-11-25 12:19:11'),(1,300,'2026-11-25 12:19:11'),(1,301,'2026-11-25 12:19:11'),(1,302,'2026-11-25 12:19:11'),(1,303,'2026-11-25 12:19:11'),(1,304,'2026-11-25 12:19:11'),(1,305,'2026-11-25 12:19:11'),(1,306,'2026-11-25 12:19:11'),(1,310,'2026-11-25 12:19:11'),(1,311,'2026-11-25 12:19:11'),(1,312,'2026-11-25 12:19:11'),(1,313,'2026-11-25 12:19:11'),(1,314,'2026-11-25 12:19:11'),(1,315,'2026-11-25 12:19:11'),(1,320,'2026-11-25 12:19:11'),(1,321,'2026-11-25 12:19:11'),(1,322,'2026-11-25 12:19:11'),(1,323,'2026-11-25 12:19:11'),(1,324,'2026-11-25 12:19:11'),(1,325,'2026-11-25 12:19:11'),(1,330,'2026-11-25 12:19:11'),(1,331,'2026-11-25 12:19:11'),(1,332,'2026-11-25 12:19:11'),(1,333,'2026-11-25 12:19:11'),(1,334,'2026-11-25 12:19:11'),(1,335,'2026-11-25 12:19:11'),(1,340,'2026-11-25 12:19:11'),(1,341,'2026-11-25 12:19:11'),(1,342,'2026-11-25 12:19:11'),(1,343,'2026-11-25 12:19:11'),(1,344,'2026-11-25 12:19:11'),(1,345,'2026-11-25 12:19:11'),(1,350,'2026-11-25 12:19:11'),(1,351,'2026-11-25 12:19:11'),(1,352,'2026-11-25 12:19:11'),(1,353,'2026-11-25 12:19:11'),(1,354,'2026-11-25 12:19:11'),(1,355,'2026-11-25 12:19:11'),(1,360,'2026-11-25 12:19:11'),(1,361,'2026-11-25 12:19:11'),(1,362,'2026-11-25 12:19:11'),(1,363,'2026-11-25 12:19:11'),(1,370,'2026-11-25 12:19:11'),(1,371,'2026-11-25 12:19:11'),(1,372,'2026-11-25 12:19:11'),(1,373,'2026-11-25 12:19:11'),(1,374,'2026-11-25 12:19:11'),(1,375,'2026-11-25 12:19:11'),(1,376,'2026-11-25 12:19:11'),(1,377,'2026-11-25 12:19:11'),(1,378,'2026-11-25 12:19:11'),(1,379,'2026-11-25 12:19:11'),(1,380,'2026-11-25 12:19:11'),(1,381,'2026-11-25 12:19:11'),(1,390,'2026-11-25 12:19:11'),(1,391,'2026-11-25 12:19:11'),(1,392,'2026-11-25 12:19:11'),(1,393,'2026-11-25 12:19:12'),(1,394,'2026-11-25 12:19:12'),(1,395,'2026-11-25 12:19:12'),(1,396,'2026-11-25 12:19:12'),(1,400,'2026-11-25 12:19:12'),(1,401,'2026-11-25 12:19:12'),(1,402,'2026-11-25 12:19:12'),(1,403,'2026-11-25 12:19:12'),(1,404,'2026-11-25 12:19:12'),(1,405,'2026-11-25 12:19:12'),(1,406,'2026-11-25 12:19:12'),(1,407,'2026-11-25 12:19:12'),(1,408,'2026-11-25 12:19:12'),(1,500,'2026-11-25 12:19:12'),(1,501,'2026-11-25 12:19:12'),(1,502,'2026-11-25 12:19:12'),(1,503,'2026-11-25 12:19:12'),(1,504,'2026-11-25 12:19:12'),(1,505,'2026-11-25 12:19:12'),(1,506,'2026-11-25 12:19:12'),(1,507,'2026-11-25 12:19:12'),(1,508,'2026-11-25 12:19:12'),(1,509,'2026-11-25 12:19:12'),(1,510,'2026-11-25 12:19:12'),(1,511,'2026-11-25 12:19:12');
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `idempotency_key` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `order_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RESOURCE',
  `resource_id` bigint unsigned DEFAULT NULL,
  `membership_plan_id` bigint unsigned DEFAULT NULL,
  `membership_duration_days` int unsigned DEFAULT NULL,
  `membership_daily_limit` int unsigned DEFAULT NULL,
  `total_amount` decimal(18,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY',
  `client_type` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_PAYMENT',
  `grant_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE',
  `expire_at` datetime NOT NULL,
  `paid_at` datetime DEFAULT NULL,
  `closed_at` datetime DEFAULT NULL,
  `close_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_order_no` (`order_no`),
  UNIQUE KEY `uk_trade_order_idempotency_key` (`idempotency_key`),
  KEY `idx_trade_order_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_trade_order_status_expire` (`status`,`expire_at`),
  KEY `idx_trade_order_resource` (`resource_id`,`user_id`,`status`),
  KEY `idx_trade_order_type_user` (`order_type`,`user_id`,`create_time`),
  KEY `idx_trade_order_membership_plan` (`membership_plan_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_order` WRITE;
/*!40000 ALTER TABLE `trade_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_order_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned DEFAULT NULL,
  `resource_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(18,2) NOT NULL,
  `owner_id` bigint unsigned DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_order_item_order_resource` (`order_id`,`resource_id`),
  KEY `idx_trade_order_item_resource` (`resource_id`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_order_item` WRITE;
/*!40000 ALTER TABLE `trade_order_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_order_item` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_payment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint unsigned NOT NULL,
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scene` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CREATED',
  `channel_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `form_html` mediumtext COLLATE utf8mb4_unicode_ci,
  `code_url` text COLLATE utf8mb4_unicode_ci,
  `h5_url` text COLLATE utf8mb4_unicode_ci,
  `channel_status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `last_query_at` datetime DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_payment_no` (`payment_no`),
  UNIQUE KEY `uk_trade_payment_order` (`order_id`),
  UNIQUE KEY `uk_trade_payment_channel_trade` (`channel`,`channel_trade_no`),
  KEY `idx_trade_payment_status_query` (`status`,`last_query_at`),
  KEY `idx_trade_payment_created` (`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_payment` WRITE;
/*!40000 ALTER TABLE `trade_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_payment` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_payment_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_payment_notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `notification_id` varchar(192) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_id` bigint unsigned DEFAULT NULL,
  `order_id` bigint unsigned DEFAULT NULL,
  `channel_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `process_status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RECEIVED',
  `raw_payload` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_payment_notification_id` (`notification_id`),
  KEY `idx_trade_payment_notification_order` (`order_id`,`create_time`),
  KEY `idx_trade_payment_notification_status` (`process_status`,`create_time`),
  KEY `fk_trade_payment_notification_payment` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_payment_notification` WRITE;
/*!40000 ALTER TABLE `trade_payment_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_payment_notification` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_reconciliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_reconciliation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reconciliation_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint unsigned NOT NULL,
  `payment_id` bigint unsigned DEFAULT NULL,
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `triggered_by` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_amount` decimal(18,2) NOT NULL,
  `channel_amount` decimal(18,2) DEFAULT NULL,
  `expected_status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel_status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` bigint unsigned DEFAULT NULL,
  `detail` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reconciled_at` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_reconciliation_no` (`reconciliation_no`),
  KEY `idx_trade_reconciliation_order_created` (`order_id`,`create_time`,`id`),
  KEY `idx_trade_reconciliation_result_created` (`result`,`create_time`,`id`),
  KEY `fk_trade_reconciliation_payment` (`payment_id`),
  KEY `fk_trade_reconciliation_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_reconciliation` WRITE;
/*!40000 ALTER TABLE `trade_reconciliation` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_reconciliation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `trade_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_refund` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint unsigned NOT NULL,
  `payment_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_REVIEW',
  `channel_refund_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel_status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_id` bigint unsigned DEFAULT NULL,
  `review_opinion` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `refunded_at` datetime DEFAULT NULL,
  `entitlement_revoked` tinyint NOT NULL DEFAULT '0',
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_refund_no` (`refund_no`),
  UNIQUE KEY `uk_trade_refund_order` (`order_id`),
  UNIQUE KEY `uk_trade_refund_channel_no` (`channel`,`channel_refund_no`),
  KEY `idx_trade_refund_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_trade_refund_status_created` (`status`,`create_time`,`id`),
  KEY `fk_trade_refund_payment` (`payment_id`),
  KEY `fk_trade_refund_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `trade_refund` WRITE;
/*!40000 ALTER TABLE `trade_refund` DISABLE KEYS */;
/*!40000 ALTER TABLE `trade_refund` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `token_version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_status_created` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_account` WRITE;
/*!40000 ALTER TABLE `user_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `follower_id` bigint unsigned NOT NULL,
  `followee_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_pair` (`follower_id`,`followee_id`),
  KEY `idx_follow_follower_created` (`follower_id`,`create_time`,`id`),
  KEY `idx_follow_followee_created` (`followee_id`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_follow` WRITE;
/*!40000 ALTER TABLE `user_follow` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_follow` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_level` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `level` int unsigned NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `min_experience` bigint unsigned NOT NULL,
  `icon` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `benefits` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` tinyint NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_level_level` (`level`),
  UNIQUE KEY `uk_user_level_experience` (`min_experience`),
  KEY `idx_user_level_status_sort` (`status`,`sort_order`,`level`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_level` WRITE;
/*!40000 ALTER TABLE `user_level` DISABLE KEYS */;
INSERT INTO `user_level` VALUES (1,1,'初识万卷',0,NULL,'基础浏览与互动',1,1,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(2,2,'勤学成员',100,NULL,'成长标识',1,2,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(3,3,'资深成员',500,NULL,'资深成长标识',1,3,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(4,4,'万卷达人',2000,NULL,'达人成长标识',1,4,'2026-11-25 12:19:11','2026-11-25 12:19:11'),(5,5,'万卷之星',10000,NULL,'星级成长标识',1,5,'2026-11-25 12:19:11','2026-11-25 12:19:11');
/*!40000 ALTER TABLE `user_level` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_membership` (
  `user_id` bigint unsigned NOT NULL,
  `plan_id` bigint unsigned DEFAULT NULL,
  `daily_resource_limit` int unsigned NOT NULL DEFAULT '0',
  `expire_at` datetime DEFAULT NULL,
  `last_order_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_membership_order` (`last_order_no`),
  KEY `idx_user_membership_expire` (`expire_at`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_membership` WRITE;
/*!40000 ALTER TABLE `user_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_membership` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profile` (
  `user_id` bigint unsigned NOT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_file_id` bigint unsigned DEFAULT NULL,
  `pending_nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pending_avatar_file_id` bigint unsigned DEFAULT NULL,
  `pending_signature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pending_remove_avatar` tinyint NOT NULL DEFAULT '0',
  `profile_audit_status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'APPROVED',
  `profile_audit_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_version` int unsigned NOT NULL DEFAULT '0',
  `signature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `gender` tinyint NOT NULL DEFAULT '0',
  `allow_like` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许他人点赞自己的内容',
  `allow_dislike` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许他人点踩自己的内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_profile_audit_status` (`profile_audit_status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_profile` WRITE;
/*!40000 ALTER TABLE `user_profile` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_profile` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_sign_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_sign_in` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `sign_date` date NOT NULL,
  `streak_days` int unsigned NOT NULL DEFAULT '1' COMMENT '签到当天连续天数',
  `points` int unsigned NOT NULL,
  `idempotency_key` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sign_in_date` (`user_id`,`sign_date`),
  UNIQUE KEY `uk_user_sign_in_idempotency` (`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_sign_in` WRITE;
/*!40000 ALTER TABLE `user_sign_in` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_sign_in` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `wallet_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_account` (
  `user_id` bigint unsigned NOT NULL,
  `available_balance` decimal(18,4) NOT NULL DEFAULT '0.0000',
  `frozen_balance` decimal(18,4) NOT NULL DEFAULT '0.0000',
  `total_income` decimal(18,4) NOT NULL DEFAULT '0.0000',
  `version` int unsigned NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_wallet_available` (`available_balance`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `wallet_account` WRITE;
/*!40000 ALTER TABLE `wallet_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `wallet_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_ledger` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biz_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `available_change` decimal(18,4) NOT NULL,
  `frozen_change` decimal(18,4) NOT NULL,
  `available_after` decimal(18,4) NOT NULL,
  `frozen_after` decimal(18,4) NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_ledger_biz` (`biz_type`,`biz_id`),
  KEY `idx_wallet_ledger_user_created` (`user_id`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `wallet_ledger` WRITE;
/*!40000 ALTER TABLE `wallet_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_ledger` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `wallet_withdrawal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_withdrawal` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `withdrawal_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `alipay_account` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AES-GCM加密后的支付宝账号',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `external_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handle_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `handled_by` bigint unsigned DEFAULT NULL,
  `handled_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_withdrawal_no` (`withdrawal_no`),
  UNIQUE KEY `uk_wallet_withdrawal_external` (`external_trade_no`),
  KEY `idx_wallet_withdrawal_user_created` (`user_id`,`create_time`,`id`),
  KEY `idx_wallet_withdrawal_status_created` (`status`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `wallet_withdrawal` WRITE;
/*!40000 ALTER TABLE `wallet_withdrawal` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_withdrawal` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
