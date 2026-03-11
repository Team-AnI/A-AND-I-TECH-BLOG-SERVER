# API-Spec-v1

코드베이스 기준(2026-03-05) 전체 API 명세.

## 0. 공통 규칙

- Base Path: `/v1`
- Posts API Prefix: `/v1/posts`
- Image API Prefix: `/v1/posts/images`
- Content-Type(JSON): `application/json`
- Content-Type(멀티파트): `multipart/form-data`
- 시간 형식: ISO-8601 (`Instant`, 예: `2026-03-05T10:00:00Z`)

### 0.1 인증/인가

- 현재 코드 기준으로 컨트롤러 레벨 인증/인가 강제 로직은 없음
- 추후 Gateway/Auth 서버에서 Bearer 토큰 정책을 적용하는 구조

### 0.2 공통 응답 래퍼

모든 API는 아래 래퍼를 사용합니다.

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-03-05T10:00:00Z"
}
```

실패 시:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "title: must not be blank"
  },
  "timestamp": "2026-03-05T10:00:00Z"
}
```

### 0.3 예외처리 모델

- Tech Blog 서버의 공통/상세 예외처리 모델은 [Error-Model-v1](Error-Model-v1) 문서에서 정의합니다.
- 본 문서(API-Spec)는 요청/응답 계약과 엔드포인트 구조만 다룹니다.

### 0.4 공통 헤더

- 요청 헤더(선택): `X-Correlation-Id`
- 응답 헤더: `X-Correlation-Id` (요청값 재사용 또는 서버 생성)

## 1. Enum

- `PostStatus`: `Draft` | `Published` | `Deleted`

## 2. 스키마

### 2.1 PostAuthorRequest

`author` 필드는 객체 또는 문자열 모두 허용합니다.

- 객체 형식
  - `id` (필수, string, 공백 불가, max 100)
  - `nickname` (선택, string, 공백-only 불가, max 50)
  - `profileImageUrl` (선택, string)
- 레거시 alias
  - `userId` == `id`
  - `thumbnailUrl` == `profileImageUrl`
- 문자열 형식
  - `"author": "u-1001"` 가능

### 2.2 CreatePostRequest (`multipart`의 `post` 파트 JSON)

- `title` (필수, not blank, max 200)
- `contentMarkdown` (필수, not blank)
- `thumbnailUrl` (선택)
- `author` (필수, `PostAuthorRequest`)
  - 레거시로 `authorId`도 허용
- `status` (선택, 기본값 `Published`)

### 2.3 PatchPostRequest (`application/json`)

- `title` (선택, min 1, max 200)
- `contentMarkdown` (선택)
- `thumbnailUrl` (선택)
- `author` (선택, `PostAuthorRequest`, `authorId` alias 허용)
- `status` (선택)

### 2.4 PostResponse

- `id` (UUID)
- `title` (string)
- `contentMarkdown` (string)
- `thumbnailUrl` (string|null)
- `author`
  - `id` (string)
  - `nickname` (string)
  - `profileImageUrl` (string|null)
- `status` (`PostStatus`)
- `createdAt` (Instant)
- `updatedAt` (Instant)

### 2.5 PagedPostResponse

- `items` (`PostResponse[]`)
- `page` (int)
- `size` (int)
- `totalElements` (long)
- `totalPages` (int)

### 2.6 ImageUploadResponse

- `url` (string)
- `key` (string, 예: `posts/<uuid>.png`)
- `contentType` (string)
- `size` (long)

## 3. Posts APIs

### 3.1 게시글 생성

- `POST /v1/posts`
- Content-Type: `multipart/form-data`
- 파트
  - `post` (필수, JSON)
  - `thumbnail` (선택, file)

Request 예시(객체 author):

```http
POST /v1/posts
Content-Type: multipart/form-data
```

`post` 파트(JSON):

```json
{
  "title": "title",
  "contentMarkdown": "content",
  "thumbnailUrl": "https://cdn.example.com/posts/thumbnail-1.webp",
  "author": {
    "id": "u-1001",
    "nickname": "neo",
    "profileImageUrl": "https://cdn.example.com/users/neo.webp"
  },
  "status": "Draft"
}
```

Request 예시(레거시 authorId):

```json
{
  "title": "title",
  "contentMarkdown": "content",
  "authorId": "u-legacy-1",
  "status": "Draft"
}
```

Success `201`:

```json
{
  "success": true,
  "data": {
    "id": "c22f7f3d-f7c8-4a63-8a3f-890d34c0fd0f",
    "title": "title",
    "contentMarkdown": "content",
    "thumbnailUrl": "https://cdn.example.com/posts/uploaded-thumb.webp",
    "author": {
      "id": "u-1001",
      "nickname": "neo",
      "profileImageUrl": "https://cdn.example.com/users/neo.webp"
    },
    "status": "Published",
    "createdAt": "2026-03-05T10:00:00Z",
    "updatedAt": "2026-03-05T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-03-05T10:00:00Z"
}
```

참고:

- `thumbnail` 파트가 있으면 업로드 URL이 `thumbnailUrl`로 우선 반영됩니다.

### 3.2 게시글 상세 조회

- `GET /v1/posts/{postId}`

Success `200`:

```json
{
  "success": true,
  "data": {
    "id": "c22f7f3d-f7c8-4a63-8a3f-890d34c0fd0f",
    "title": "title",
    "contentMarkdown": "content",
    "thumbnailUrl": "https://cdn.example.com/posts/thumbnail-detail.webp",
    "author": {
      "id": "u-2001",
      "nickname": "상욱",
      "profileImageUrl": "https://cdn.example.com/users/sangwook.webp"
    },
    "status": "Published",
    "createdAt": "2026-03-05T10:00:00Z",
    "updatedAt": "2026-03-05T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-03-05T10:00:00Z"
}
```

### 3.3 게시글 목록 조회(게시용)

- `GET /v1/posts?page=0&size=20&status=Published`
- Query
  - `page` 기본 0, min 0
  - `size` 기본 20, min 1, max 100
  - `status` 선택(`Published`/`Deleted`/`Draft`)

주의:

- `status=Draft`는 이 API에서 금지(`400`, 메시지: `draft posts are only available in draft list`)
- `status` 미지정 시 `Published`가 기본값

Success `200`:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "c22f7f3d-f7c8-4a63-8a3f-890d34c0fd0f",
        "title": "title",
        "contentMarkdown": "content",
        "thumbnailUrl": "https://cdn.example.com/posts/thumbnail-list.webp",
        "author": {
          "id": "u-1003",
          "nickname": "neo",
          "profileImageUrl": "https://cdn.example.com/users/neo.webp"
        },
        "status": "Published",
        "createdAt": "2026-03-05T10:00:00Z",
        "updatedAt": "2026-03-05T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "2026-03-05T10:00:00Z"
}
```

### 3.4 게시글 목록 조회(초안 전용)

- `GET /v1/posts/drafts?page=0&size=20`
- Query
  - `page` 기본 0, min 0
  - `size` 기본 20, min 1, max 100

Success `200`:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "c22f7f3d-f7c8-4a63-8a3f-890d34c0fd0f",
        "title": "draft title",
        "contentMarkdown": "draft content",
        "thumbnailUrl": null,
        "author": {
          "id": "u-1004",
          "nickname": "neo",
          "profileImageUrl": null
        },
        "status": "Draft",
        "createdAt": "2026-03-05T10:00:00Z",
        "updatedAt": "2026-03-05T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "2026-03-05T10:00:00Z"
}
```

### 3.5 게시글 수정

- `PATCH /v1/posts/{postId}`
- Content-Type: `application/json`

Request:

```json
{
  "title": "updated",
  "contentMarkdown": "updated-content",
  "thumbnailUrl": "https://cdn.example.com/posts/thumbnail-updated.webp",
  "author": {
    "id": "u-1005",
    "nickname": "neo",
    "profileImageUrl": "https://cdn.example.com/users/neo.webp"
  },
  "status": "Published"
}
```

Success `200`:

```json
{
  "success": true,
  "data": {
    "id": "c22f7f3d-f7c8-4a63-8a3f-890d34c0fd0f",
    "title": "updated",
    "contentMarkdown": "updated-content",
    "thumbnailUrl": "https://cdn.example.com/posts/thumbnail-updated.webp",
    "author": {
      "id": "u-1005",
      "nickname": "neo",
      "profileImageUrl": "https://cdn.example.com/users/neo.webp"
    },
    "status": "Published",
    "createdAt": "2026-03-05T10:00:00Z",
    "updatedAt": "2026-03-05T10:10:00Z"
  },
  "error": null,
  "timestamp": "2026-03-05T10:10:00Z"
}
```

### 3.6 게시글 삭제

- `DELETE /v1/posts/{postId}`
- 동작: soft delete (`status=Deleted`)

Success `200`:

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "error": null,
  "timestamp": "2026-03-05T10:20:00Z"
}
```

## 4. Images API

### 4.1 이미지 업로드

- `POST /v1/posts/images`
- Content-Type: `multipart/form-data`
- 파트
  - `file` (필수)

성공 `200`:

```json
{
  "success": true,
  "data": {
    "url": "https://bucket.s3.us-east-1.amazonaws.com/posts/abc.png",
    "key": "posts/abc.png",
    "contentType": "image/png",
    "size": 4
  },
  "error": null,
  "timestamp": "2026-03-05T10:30:00Z"
}
```

업로드 정책(기본값):

- max-size: `5MB` (`5242880` bytes)
- allowed types: `image/png`, `image/jpeg`, `image/gif`, `image/webp`

## 5. 구현 기준 파일

- Controller
  - `src/main/kotlin/com/aandiclub/tech/blog/presentation/post/PostController.kt`
  - `src/main/kotlin/com/aandiclub/tech/blog/presentation/image/ImageController.kt`
- DTO
  - `src/main/kotlin/com/aandiclub/tech/blog/presentation/post/dto/*.kt`
  - `src/main/kotlin/com/aandiclub/tech/blog/presentation/image/dto/ImageUploadResponse.kt`
- 응답/에러
  - `src/main/kotlin/com/aandiclub/tech/blog/common/api/ApiResponse.kt`
  - `src/main/kotlin/com/aandiclub/tech/blog/common/error/GlobalExceptionHandler.kt`
