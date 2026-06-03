-- 私信表结构迁移（lfc_dev）
-- 原因：旧 message 表是系统通知结构，无法直接加 conversation_id 外键
-- 用法：mysql -u root -p lfc_dev < scripts/migrate-im-schema.sql

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 若 notification 表不存在，把旧 message（系统通知）迁过去
CREATE TABLE IF NOT EXISTS `notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `type` enum('SYSTEM','ACTIVITY_SUBMITTED','ACTIVITY_APPROVED','ACTIVITY_REJECTED','ACTIVITY_JOIN') NOT NULL,
  `related_type` enum('ACTIVITY','SYSTEM') DEFAULT NULL,
  `related_id` int DEFAULT NULL,
  `is_read` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `IDX_notification_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `notification` (
  `id`, `user_id`, `title`, `content`, `type`, `related_type`, `related_id`, `is_read`, `created_at`
)
SELECT
  `id`, `user_id`, `title`, `content`, `type`, `related_type`, `related_id`, `is_read`, `created_at`
FROM `message`
WHERE EXISTS (
  SELECT 1
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'message'
    AND COLUMN_NAME = 'user_id'
)
ON DUPLICATE KEY UPDATE `notification`.`id` = `notification`.`id`;

-- 2. 删除旧的 message 表（系统通知），避免与私信 chat_message 冲突
DROP TABLE IF EXISTS `message`;

-- 3. 若 conversation 处于半迁移状态，清掉无效的 chat_message（若曾创建）
DROP TABLE IF EXISTS `chat_message`;

SET FOREIGN_KEY_CHECKS = 1;

-- 重启 Nest 后 TypeORM synchronize 会自动创建 conversation、chat_message 表
