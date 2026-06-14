ALTER TABLE post
  ADD COLUMN IF NOT EXISTS category VARCHAR(32) NOT NULL DEFAULT '校园生活' AFTER title,
  ADD KEY IF NOT EXISTS idx_post_category (category, created_at);
