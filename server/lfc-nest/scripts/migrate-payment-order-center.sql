-- 订单中心：评价 + 售后 + REFUNDED 状态（在 migrate-payment-royalty-settle.sql 之后执行）

ALTER TABLE `payment_order`
  MODIFY `status` ENUM(
    'PENDING',
    'PAID',
    'CONFIRMED',
    'SETTLED',
    'REFUNDED',
    'CLOSED',
    'FAILED'
  ) NOT NULL DEFAULT 'PENDING';

CREATE TABLE IF NOT EXISTS `payment_order_review` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `payment_order_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `rating` TINYINT NOT NULL COMMENT '1-5 星',
  `content` VARCHAR(500) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `IDX_payment_order_review_order` (`payment_order_id`),
  CONSTRAINT `FK_payment_order_review_order` FOREIGN KEY (`payment_order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_payment_order_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_after_sales` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `payment_order_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'REFUNDING', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  `refund_amount` DECIMAL(10, 2) NOT NULL,
  `processed_at` DATETIME NULL,
  `reject_reason` VARCHAR(255) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `IDX_payment_after_sales_order` (`payment_order_id`),
  KEY `IDX_payment_after_sales_user` (`user_id`),
  CONSTRAINT `FK_payment_after_sales_order` FOREIGN KEY (`payment_order_id`) REFERENCES `payment_order` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_payment_after_sales_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
