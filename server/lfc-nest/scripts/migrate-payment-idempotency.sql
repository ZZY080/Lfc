-- 支付幂等与并发控制（在 migrate-payment-royalty-settle.sql 之后执行）

ALTER TABLE `payment_order`
  ADD COLUMN IF NOT EXISTS `active_key` VARCHAR(128) NULL COMMENT '非终态订单唯一键 user+biz' AFTER `out_trade_no`,
  ADD COLUMN IF NOT EXISTS `channel_trade_key` VARCHAR(96) NULL COMMENT '渠道交易唯一键，防重复入账' AFTER `trade_no`,
  ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁' AFTER `updated_at`;

ALTER TABLE `payment_order`
  ADD UNIQUE KEY IF NOT EXISTS `IDX_payment_order_active_key` (`active_key`),
  ADD UNIQUE KEY IF NOT EXISTS `IDX_payment_order_channel_trade_key` (`channel_trade_key`);

-- 回填已有非终态订单的 active_key（可选，避免旧单重复下单）
UPDATE `payment_order`
SET `active_key` = CONCAT(`biz_type`, ':', `biz_id`, ':', `user_id`)
WHERE `active_key` IS NULL
  AND `status` IN ('PENDING', 'PAID', 'CONFIRMED', 'SETTLED');

UPDATE `payment_order`
SET `channel_trade_key` = CONCAT(`channel`, ':', `trade_no`)
WHERE `channel_trade_key` IS NULL
  AND `trade_no` IS NOT NULL
  AND `trade_no` <> '';
