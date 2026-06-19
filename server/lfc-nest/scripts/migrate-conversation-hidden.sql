-- 会话左滑删除：按用户隐藏会话（不删除聊天记录）
-- 用法：mysql -u root -p lfc_dev < scripts/migrate-conversation-hidden.sql

ALTER TABLE `conversation`
  ADD COLUMN `user_one_hidden` tinyint NOT NULL DEFAULT 0 AFTER `user_two_unread_count`,
  ADD COLUMN `user_two_hidden` tinyint NOT NULL DEFAULT 0 AFTER `user_one_hidden`;
