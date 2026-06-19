-- 发现页频道目录表（我的频道 / 推荐频道数据来源）
-- 用法：mysql -u root -p lfc_dev < scripts/migrate-feed-channel.sql

CREATE TABLE IF NOT EXISTS `feed_channel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `is_recommend` tinyint NOT NULL DEFAULT 0,
  `default_in_my` tinyint NOT NULL DEFAULT 0,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ_feed_channel_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
