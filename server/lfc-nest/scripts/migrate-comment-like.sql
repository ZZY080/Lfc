ALTER TABLE post_comment
  ADD COLUMN IF NOT EXISTS like_count INT NOT NULL DEFAULT 0 AFTER content;

CREATE TABLE IF NOT EXISTS post_comment_like (
  id INT AUTO_INCREMENT PRIMARY KEY,
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_post_comment_like (comment_id, user_id),
  KEY idx_post_comment_like_user (user_id),
  CONSTRAINT fk_post_comment_like_comment
    FOREIGN KEY (comment_id) REFERENCES post_comment(id) ON DELETE CASCADE,
  CONSTRAINT fk_post_comment_like_user
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
