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
- `DEPLOY_PORT` (optional, default: `22`)
- `POSTGRES_DB` (optional, default: `tech_blog`)
- `POSTGRES_USER` (optional, default: `tech_blog`)

### Repository Secrets (`Settings > Secrets and variables > Actions > Secrets`)
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `POSTGRES_PASSWORD`

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
- Current workflow logs into ECR on the instance using `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` passed from GitHub Secrets.
