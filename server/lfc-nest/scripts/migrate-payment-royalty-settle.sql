-- 商家分账 + 确认收货后结算（在 migrate-payment-schema.sql 之后执行）

ALTER TABLE `payment_order`
  MODIFY `status` ENUM(
    'PENDING',
    'PAID',
    'CONFIRMED',
    'SETTLED',
    'CLOSED',
    'FAILED'
  ) NOT NULL DEFAULT 'PENDING';

ALTER TABLE `payment_order`
  ADD COLUMN IF NOT EXISTS `confirmed_at` DATETIME NULL COMMENT '买家确认收货时间' AFTER `paid_at`,
  ADD COLUMN IF NOT EXISTS `settled_at` DATETIME NULL COMMENT '分账完成时间' AFTER `confirmed_at`,
  ADD COLUMN IF NOT EXISTS `auto_confirm_at` DATETIME NULL COMMENT '自动确认收货截止时间' AFTER `settled_at`;

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `alipay_royalty_bound_at` DATETIME NULL COMMENT '分账关系绑定时间' AFTER `alipay_bound_at`,
  ADD COLUMN IF NOT EXISTS `alipay_user_id` VARCHAR(64) NULL COMMENT '支付宝 userId（OAuth 授权）' AFTER `alipay_login_id`;

-- 兼容旧库：历史上 alipay_user_id 可能是 VARCHAR(32)，OAuth 返回值可能更长
ALTER TABLE `user`
  MODIFY COLUMN `alipay_user_id` VARCHAR(64) NULL COMMENT '支付宝 userId（OAuth 授权）';
