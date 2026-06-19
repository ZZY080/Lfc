-- 用户封禁 + 评论可见性（在已有库上执行）

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `status` ENUM('ACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE' AFTER `role`,
  ADD COLUMN IF NOT EXISTS `ban_reason` VARCHAR(255) NULL AFTER `status`,
  ADD COLUMN IF NOT EXISTS `banned_at` DATETIME NULL AFTER `ban_reason`;

ALTER TABLE `post_comment`
  ADD COLUMN IF NOT EXISTS `is_visible` TINYINT(1) NOT NULL DEFAULT 1 AFTER `like_count`;

ALTER TABLE `activity`
  ADD COLUMN IF NOT EXISTS `review_comment` TEXT NULL AFTER `status`;
