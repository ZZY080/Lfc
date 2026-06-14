ALTER TABLE post_comment
  ADD COLUMN IF NOT EXISTS root_id INT NULL AFTER parent_id,
  ADD KEY IF NOT EXISTS idx_post_comment_root (post_id, root_id, created_at);

UPDATE post_comment
SET root_id = id
WHERE parent_id IS NULL AND root_id IS NULL;

UPDATE post_comment AS child
INNER JOIN post_comment AS parent ON child.parent_id = parent.id
SET child.root_id = parent.root_id
WHERE child.root_id IS NULL AND parent.root_id IS NOT NULL;
