# A-AND-I-TECH-BLOG-SERVER

## CI/CD (GitHub Actions)

### 1) CI - test only
- Trigger:
  - push to `main`
  - merged PR into `main`
- Workflow: `.github/workflows/ci-test.yml`
- Action: `./gradlew test --no-daemon`

### 2) CD - deploy only on tag
- Trigger:
  - push tag matching `vX.Y.Z` (example: `v1.2.3`)
- Workflow: `.github/workflows/deploy-tag.yml`
- Flow:
  - Docker image build
  - Push image to ECR
  - SSH to instance
  - `docker compose pull && docker compose up -d`

## Required GitHub repository settings

### Repository Variables (`Settings > Secrets and variables > Actions > Variables`)
- `AWS_REGION` (example: `ap-northeast-2`)
- `ECR_REPOSITORY` (example: `tech-blog`)
- `APP_DIR` (optional, default: `/opt/tech-blog`)
- `AWS_PORT` (optional, default: `22`)
- `POSTGRES_DB` (optional, default: `tech_blog`)
- `POSTGRES_USER` (optional, default: `tech_blog`)
- `APP_USER_EVENTS_ENABLED` (optional, default: `false`)
- `APP_USER_EVENTS_REGION` (optional, default: `AWS_REGION`)
- `APP_USER_EVENTS_WAIT_TIME_SECONDS` (optional, default: `20`)
- `APP_USER_EVENTS_MAX_MESSAGES` (optional, default: `10`)
- `APP_USER_EVENTS_POLL_DELAY_MS` (optional, default: `1000`)

### Repository Secrets (`Settings > Secrets and variables > Actions > Secrets`)
- `AWS_ROLE_TO_ASSUME`
- `AWS_HOST`
- `AWS_USER`
- `AWS_SSH_KEY`
- `POSTGRES_PASSWORD`
- `APP_S3_BUCKET`
- `APP_S3_REGION`
- `APP_S3_PUBLIC_BASE_URL`
- `APP_USER_EVENTS_QUEUE_URL` (required if `APP_USER_EVENTS_ENABLED=true`)

## Release example

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Local docker-compose run

```bash
cp .env.example .env
docker build -t tech-blog:local .
docker compose up -d
```

## Notes
- Target instance must have `docker`, `docker compose`, and `aws cli`.
- Current workflow uses GitHub OIDC + `AWS_ROLE_TO_ASSUME` to authenticate in GitHub Actions.
