# ERD

## posts

- `id` UUID PK
- `title` VARCHAR(200) NOT NULL
- `content_markdown` TEXT NOT NULL
- `author_id` UUID NOT NULL
- `status` VARCHAR(20) NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

## indexes

- `idx_posts_status` (`status`)
- `idx_posts_created_at` (`created_at` DESC)

## rules

- soft delete: `status = Deleted`
- 조회 API 기본 정책: `Deleted` 제외
