ALTER TABLE posts
ALTER COLUMN author_id TYPE VARCHAR(100) USING author_id::text;

CREATE TABLE users (
  id VARCHAR(100) PRIMARY KEY,
  nickname VARCHAR(50) NOT NULL,
  thumbnail_url TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_nickname ON users (nickname);
CREATE INDEX idx_posts_author_id ON posts (author_id);

INSERT INTO users (id, nickname, thumbnail_url, created_at, updated_at)
SELECT DISTINCT
  p.author_id,
  'user-' || substring(p.author_id, 1, 8),
  NULL,
  now(),
  now()
FROM posts p
LEFT JOIN users u ON u.id = p.author_id
WHERE u.id IS NULL;

ALTER TABLE posts
ADD CONSTRAINT fk_posts_author_id
FOREIGN KEY (author_id) REFERENCES users (id);
