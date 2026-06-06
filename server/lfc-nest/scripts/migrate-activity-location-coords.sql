-- 活动地点坐标（高德定位 / 导航）

ALTER TABLE `activity`
  ADD COLUMN IF NOT EXISTS `latitude` DOUBLE NULL COMMENT '纬度' AFTER `location`,
  ADD COLUMN IF NOT EXISTS `longitude` DOUBLE NULL COMMENT '经度' AFTER `latitude`;
