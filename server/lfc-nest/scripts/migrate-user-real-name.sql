-- 用户注册真实姓名（在已有 user 表上执行）

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `real_name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '真实姓名' AFTER `student_id`;

-- 已有用户若无真实姓名，可用昵称或学号占位（按需手动修正）
UPDATE `user`
SET `real_name` = COALESCE(NULLIF(`nickname`, ''), `student_id`)
WHERE `real_name` = '' OR `real_name` IS NULL;
