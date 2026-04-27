ALTER TABLE posts
ADD COLUMN IF NOT EXISTS scheduled_publish_at TIMESTAMPTZ NULL,
ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ NULL;

UPDATE posts
SET published_at = created_at
WHERE status = 'Published'
  AND published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_posts_scheduled_publish_at
ON posts (scheduled_publish_at)
WHERE status = 'Scheduled'
  AND scheduled_publish_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_posts_published_at
ON posts (published_at DESC);
