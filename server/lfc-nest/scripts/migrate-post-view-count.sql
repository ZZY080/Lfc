ALTER TABLE post
  ADD COLUMN view_count INT NOT NULL DEFAULT 0 AFTER comment_count;
