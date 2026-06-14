-- 私信消息类型：支持附带笔记/活动卡片
ALTER TABLE `chat_message`
  MODIFY COLUMN `message_type`
    ENUM('TEXT', 'IMAGE', 'VIDEO', 'PRODUCT', 'POST', 'ACTIVITY')
    NOT NULL DEFAULT 'TEXT';
