ALTER TABLE posts
ADD COLUMN IF NOT EXISTS summary VARCHAR(300);

UPDATE posts
SET summary = LEFT(
  COALESCE(
    NULLIF(regexp_replace(TRIM(COALESCE(content_markdown, '')), '\s+', ' ', 'g'), ''),
    title
  ),
  300
)
WHERE summary IS NULL OR BTRIM(summary) = '';

ALTER TABLE posts
ALTER COLUMN summary SET NOT NULL;
