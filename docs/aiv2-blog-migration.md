# A&I v2 Blog API Migration Guide

## Scope
- Service: `tech.blog`
- Strategy: keep v1 intact, add v2 side-by-side
- Rule: no breaking change to existing `/v1/**`

## Implemented structure

### v1 (unchanged)
- `com.aandiclub.tech.blog.presentation.post.PostController`
- `com.aandiclub.tech.blog.presentation.image.ImageController`
- `com.aandiclub.tech.blog.common.error.GlobalExceptionHandler`
- `com.aandiclub.tech.blog.common.api.ApiResponse`

### v2 (new)
- `com.aandiclub.tech.blog.presentation.v2.post.V2PostController`
- `com.aandiclub.tech.blog.presentation.v2.image.V2ImageController`
- `com.aandiclub.tech.blog.common.api.v2.AiV2ApiResponse`
- `com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver`
- `com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler`
- `com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper`

## Why this split is safe
- Controllers are version-separated by URI and package.
- v1 DTO and response format remain untouched.
- Business logic is reused through existing `PostService` and `ImageUploadService`.
- v2-only concerns are isolated in presenter/controller concerns:
  - envelope
  - header parsing
  - error mapping
  - response DTO shape

## Endpoint mapping

| Capability | v1 | v2 |
|---|---|---|
| Create post | `POST /v1/posts` | `POST /v2/posts` |
| Get post | `GET /v1/posts/{postId}` | `GET /v2/posts/{postId}` |
| List posts | `GET /v1/posts` | `GET /v2/posts` |
| My posts | `GET /v1/posts/me` | `GET /v2/posts/me` |
| Drafts | `GET /v1/posts/drafts` | `GET /v2/posts/drafts` |
| My drafts | `GET /v1/posts/drafts/me` | `GET /v2/posts/drafts/me` |
| Patch post | `PATCH /v1/posts/{postId}` | `PATCH /v2/posts/{postId}` |
| Add collaborator | `POST /v1/posts/{postId}/collaborators` | `POST /v2/posts/{postId}/collaborators` |
| Delete post | `DELETE /v1/posts/{postId}` | `DELETE /v2/posts/{postId}` |
| Upload image | `POST /v1/posts/images` | `POST /v2/posts/images` |

## v2 header policy
- required: `deviceOS`
- required: `Authenticate: Bearer {accessToken}`
- required: `timestamp` (ISO-8601 instant)
- optional: `salt`

Resolved by:
- `common/api/v2/AiV2RequestContextResolver.kt`

## v2 response policy
```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-09T12:00:00Z"
}
```

Error shape:
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

## Shared vs separated layers

### Shared
- `PostService`, `PostServiceImpl`
- `ImageUploadService`, `ImageUploadServiceImpl`
- repositories
- domain models
- `AuthTokenService`

### Separated
- controller
- response envelope
- header resolver
- exception handler
- error code mapper
- v2 response DTOs

## Request differences

### Add collaborator
- v1 body supports `ownerId`
- v2 body removes that field and derives requester from `Authenticate`

Example v2 body:
```json
{
  "collaborator": {
    "id": "u-2002",
    "nickname": "collab"
  }
}
```

## Error mapping notes
- error catalog currently implemented in `AiV2ErrorCatalog`
- blog service uses service prefix `6`
- common/header/internal errors use service prefix `9`
- when a domain/service exception reason changes, update `AiV2ErrorMapper`

## Test status
Targeted tests passed:
- `ErrorHandlingTest`
- `PostControllerTest`
- `ImageControllerTest`
- `AiV2ErrorHandlingTest`
- `V2PostControllerTest`
- `V2ImageControllerTest`

Note:
- Full `./gradlew test` still fails on pre-existing `BootstrapConfigurationTest` because Docker/Testcontainers are unavailable in the current environment.

## Rollout checklist
1. deploy v2 endpoints without routing traffic
2. publish v2 spec to client teams
3. connect one v2 canary client
4. observe 4xx/5xx and header compliance
5. migrate additional clients gradually
6. freeze v1 feature additions when v2 adoption is sufficient
7. set v1 sunset plan later

## Follow-up refactor candidates
- move service return types from presentation DTO to domain/usecase output model
- promote v2 header validation into SecurityWebFilter when policy is stable
- add replay protection for `timestamp` + `salt`
- split OpenAPI docs into explicit v1/v2 groups
