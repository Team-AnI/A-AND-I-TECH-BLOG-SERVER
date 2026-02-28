CREATE TABLE post_collaborators (
  id UUID PRIMARY KEY,
  post_id UUID NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_post_collaborators_post_id
    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_post_collaborators_user_id
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_post_collaborators_post_id_user_id
  ON post_collaborators (post_id, user_id);

CREATE INDEX idx_post_collaborators_user_id
  ON post_collaborators (user_id);
