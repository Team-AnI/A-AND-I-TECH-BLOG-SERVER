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

## Dynamic OG share endpoint

- Endpoint: `GET /share/articles/{postId}`
- Purpose: returns server-rendered HTML with per-article Open Graph / Twitter meta tags for crawlers, then redirects users to `/articles/{postId}` via `meta refresh`
- Only `Published` blog posts are exposed
- Existing article detail APIs can also include a `share` object (`shareUrl`, `clientUrl`, `title`, `description`, `imageUrl`) for app/share-button usage
- Recommended public routing:
  - SPA: `/articles/**` -> Firebase Hosting / Flutter Web
  - Share HTML: `/share/articles/**` -> this Spring server
- Required env/config:
  - `APP_SHARE_PUBLIC_BASE_URL`: public site base URL used for `og:url` and redirect URL
  - `APP_SHARE_DEFAULT_OG_IMAGE_URL`: absolute fallback OG image URL
  - `APP_SHARE_DEFAULT_DESCRIPTION`: fallback description
  - cache tuning via `APP_SHARE_CACHE_*`

## A&I v2 API migration summary

### v1 / v2 coexistence
- Existing v1 API remains unchanged.
- New A&I-compatible API is provided under `/v2/**`.
- Current grouped OpenAPI docs:
  - `/v3/api-docs/v1`
  - `/v3/api-docs/v2`

### v2 required headers
- `deviceOS`: required
- `Authenticate`: required, format `Bearer {accessToken}`
- `timestamp`: required, ISO-8601 instant
- `salt`: optional

### v2 response envelope
```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-09T12:00:00Z"
}
```

Failure response:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": 60501,
    "message": "post not found",
    "value": "post not found",
    "alert": "post not found"
  },
  "timestamp": "2026-04-09T12:00:00Z"
}
```

> Note: the actual response field name is `error.alert`. This document describes it as the user-facing `alertMessage`.

## A&I v2 error guide

### Common policy
- `message`: developer-facing summary for client/server debugging
- `value`: raw reason/detail value used for diagnosis
- `alert`: user-facing alert message currently returned by the server
- Current implementation uses the same English text for `message` and `alert` in most cases.

### Implemented error codes for Blog/Common

| Code | Scenario | Developer message (`error.message`) | User alertMessage (`error.alert`) |
|---|---|---|---|
| 90101 | `Authenticate` header missing | Authenticate header is required | Authenticate header is required |
| 90102 | `Authenticate` header invalid / bearer parsing failed / token invalid | Authenticate header must contain a valid Bearer token | Authenticate header must contain a valid Bearer token |
| 90301 | `deviceOS` header missing | deviceOS header is required | deviceOS header is required |
| 90302 | `timestamp` header missing | timestamp header is required | timestamp header is required |
| 90303 | `timestamp` format invalid | timestamp header must be ISO-8601 instant | timestamp header must be ISO-8601 instant |
| 90304 | request body parse failure | request body is invalid | request body is invalid |
| 90801 | unexpected internal exception | internal server error | internal server error |
| 90701 | external system unavailable | external system unavailable | external system unavailable |
| 60200 | generic forbidden fallback | forbidden | forbidden |
| 60201 | non-owner/non-collaborator edit attempt | only post owner or collaborator can edit | only post owner or collaborator can edit |
| 60202 | non-owner collaborator list modification | only post owner can modify collaborators | only post owner can modify collaborators |
| 60203 | non-owner collaborator add attempt | only post owner can add collaborators | only post owner can add collaborators |
| 60301 | validation failure | validation failed | validation failed |
| 60302 | attempt to change primary author | primary author cannot be changed | primary author cannot be changed |
| 60303 | blank title / title required | title is required | title is required |
| 60304 | published post missing content | contentMarkdown is required for published post | contentMarkdown is required for published post |
| 60305 | owner included as collaborator | owner is already the primary author | owner is already the primary author |
| 60306 | new collaborator without nickname | collaborator nickname is required for new user | collaborator nickname is required for new user |
| 60401 | draft queried through non-draft list API | draft posts are only available in draft list | draft posts are only available in draft list |
| 60501 | post lookup miss | post not found | post not found |
| 60701 | image upload upstream failure | image upload failed | image upload failed |

### Recommended client handling
- Use `success` first.
- If `success=false`, parse `error.code` as the primary branch condition.
- Show `error.alert` to the user.
- Log `error.message` and `error.value` for debugging.

### Related docs
- `docs/aiv2-blog-migration.md`
- `docs/aiv2-error-codes.md`
- `docs/aiv2-service-template.md`
