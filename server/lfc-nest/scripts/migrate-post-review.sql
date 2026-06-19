-- 帖子审核状态 + 通知类型扩展

ALTER TABLE `post`
  ADD COLUMN IF NOT EXISTS `status` ENUM('PENDING','APPROVED','REJECTED','OFF_SHELF') NOT NULL DEFAULT 'APPROVED' AFTER `is_visible`,
  ADD COLUMN IF NOT EXISTS `review_comment` TEXT NULL AFTER `status`;

UPDATE `post` SET `status` = 'APPROVED' WHERE `is_visible` = 1;
UPDATE `post` SET `status` = 'OFF_SHELF' WHERE `is_visible` = 0;

ALTER TABLE `notification`
  MODIFY COLUMN `type` ENUM(
    'SYSTEM',
    'ACTIVITY_SUBMITTED',
    'ACTIVITY_APPROVED',
    'ACTIVITY_REJECTED',
    'ACTIVITY_JOIN',
    'POST_SUBMITTED',
    'POST_APPROVED',
    'POST_REJECTED'
  ) NOT NULL;

ALTER TABLE `notification`
  MODIFY COLUMN `related_type` ENUM('ACTIVITY', 'POST', 'SYSTEM') NULL;
