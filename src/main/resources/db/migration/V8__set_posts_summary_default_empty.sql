UPDATE posts
SET summary = ''
WHERE summary IS NULL OR BTRIM(summary) = '';

ALTER TABLE posts
ALTER COLUMN summary SET DEFAULT '',
ALTER COLUMN summary SET NOT NULL;
