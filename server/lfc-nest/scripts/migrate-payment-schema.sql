-- 支付与商品相关表结构（若未开启 MYSQL_SYNCHRONIZE 可手动执行）

ALTER TABLE `activity`
  ADD COLUMN IF NOT EXISTS `fee` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '报名费用，0 表示免费' AFTER `max_participants`;

CREATE TABLE IF NOT EXISTS `post_product` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `post_id` INT NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `original_price` DECIMAL(10, 2) NULL,
  `category` ENUM('SECOND_HAND', 'DIGITAL', 'BOOK', 'DAILY', 'OTHER') NOT NULL DEFAULT 'SECOND_HAND',
  `condition` ENUM('BRAND_NEW', 'LIKE_NEW', 'GOOD', 'FAIR') NOT NULL DEFAULT 'GOOD',
  `delivery_method` ENUM('PICKUP', 'EXPRESS', 'BOTH') NOT NULL DEFAULT 'PICKUP',
  `status` ENUM('ON_SALE', 'SOLD', 'OFF_SHELF') NOT NULL DEFAULT 'ON_SALE',
  `buyer_id` INT NULL,
  `sold_at` DATETIME NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `IDX_post_product_post_id` (`post_id`),
  CONSTRAINT `FK_post_product_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `out_trade_no` VARCHAR(64) NOT NULL,
  `user_id` INT NOT NULL,
  `payee_id` INT NOT NULL COMMENT 'C2C 收款方：闲置卖家或活动发起人',
  `biz_type` ENUM('ACTIVITY_JOIN', 'POST_PRODUCT_PURCHASE') NOT NULL,
  `biz_id` INT NOT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `platform_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '平台服务费',
  `payee_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '卖家实际到账',
  `subject` VARCHAR(128) NOT NULL,
  `channel` ENUM('ALIPAY', 'WECHAT') NOT NULL DEFAULT 'ALIPAY',
  `status` ENUM('PENDING', 'PAID', 'CLOSED', 'FAILED') NOT NULL DEFAULT 'PENDING',
  `trade_no` VARCHAR(64) NULL,
  `paid_at` DATETIME NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `IDX_payment_order_out_trade_no` (`out_trade_no`),
  KEY `IDX_payment_order_user_id` (`user_id`),
  KEY `IDX_payment_order_payee_id` (`payee_id`),
  CONSTRAINT `FK_payment_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_payment_order_payee` FOREIGN KEY (`payee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有 payment_order 表时，可单独执行：
-- ALTER TABLE `payment_order` MODIFY `biz_type` ENUM('ACTIVITY_JOIN', 'POST_PRODUCT_PURCHASE') NOT NULL;
-- ALTER TABLE `payment_order` ADD COLUMN IF NOT EXISTS `channel` ENUM('ALIPAY', 'WECHAT') NOT NULL DEFAULT 'ALIPAY' AFTER `subject`;
-- ALTER TABLE `payment_order` ADD COLUMN IF NOT EXISTS `payee_id` INT NOT NULL COMMENT 'C2C 收款方' AFTER `user_id`;
-- ALTER TABLE `payment_order` ADD CONSTRAINT `FK_payment_order_payee` FOREIGN KEY (`payee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `payment_order` ADD COLUMN IF NOT EXISTS `platform_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '平台服务费' AFTER `amount`;
-- ALTER TABLE `payment_order` ADD COLUMN IF NOT EXISTS `payee_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '卖家实际到账' AFTER `platform_fee`;
-- ALTER TABLE `payment_payout` ADD COLUMN IF NOT EXISTS `platform_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER `amount`;

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `alipay_login_id` VARCHAR(64) NULL COMMENT '支付宝登录号' AFTER `show_likes_public`,
  ADD COLUMN IF NOT EXISTS `alipay_real_name` VARCHAR(32) NULL COMMENT '支付宝实名' AFTER `alipay_login_id`,
  ADD COLUMN IF NOT EXISTS `alipay_bound_at` DATETIME NULL COMMENT '支付宝绑定时间' AFTER `alipay_real_name`;

CREATE TABLE IF NOT EXISTS `payment_payout` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `out_biz_no` VARCHAR(64) NOT NULL,
  `payment_order_id` INT NOT NULL,
  `payee_id` INT NOT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `platform_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0,
  `status` ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
  `alipay_order_id` VARCHAR(64) NULL,
  `error_message` VARCHAR(255) NULL,
  `settled_at` DATETIME NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `IDX_payment_payout_out_biz_no` (`out_biz_no`),
  KEY `IDX_payment_payout_payment_order_id` (`payment_order_id`),
  CONSTRAINT `FK_payment_payout_order` FOREIGN KEY (`payment_order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_payment_payout_payee` FOREIGN KEY (`payee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
