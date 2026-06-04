CREATE TABLE IF NOT EXISTS activity_like (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_activity_like (activity_id, user_id),
  KEY idx_activity_like_user (user_id),
  CONSTRAINT fk_activity_like_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE,
  CONSTRAINT fk_activity_like_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activity_favorite (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_activity_favorite (activity_id, user_id),
  KEY idx_activity_favorite_user (user_id),
  CONSTRAINT fk_activity_favorite_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE,
  CONSTRAINT fk_activity_favorite_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);
