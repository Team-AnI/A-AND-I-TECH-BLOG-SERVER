# Error-Model-v1

Tech Blog 서버의 **현재 구현 기준 에러 모델 설명 문서**입니다.

## 0. 이 문서가 필요한 이유

Tech Blog 서버는 게시글 CRUD와 이미지 업로드, S3 연동을 담당합니다.
도메인 자체는 비교적 단순하지만, 이미지 업로드와 외부 저장소 연동이 있어 오류 해석이 예상보다 다양합니다.

이 문서는 "현재 Blog 서버가 어떤 방식으로 에러를 반환하는지"와 "왜 아직 공통 taxonomy와 완전히 같지 않은지"를 설명합니다.
공통 기준 문서는 Team-AnI `.github`의 `Backend API Common Exception Handling Guide v1`를 따릅니다.

## 1. 문서 성격

이 문서는 **설명 문서**입니다.

- 현재 구현된 에러 응답 구조와 예외 매핑 상태를 설명합니다.
- 공통 표준 대비 부족한 점과 향후 정렬 방향을 분명히 적습니다.
- 아직 구현되지 않은 세부 코드는 계획으로만 다룹니다.

## 2. 현재 클라이언트가 신뢰할 수 있는 계약

### 2.1 응답 Envelope

Tech Blog 서버는 `ApiResponse` 래퍼를 사용합니다.

실패 예시:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "title: must not be blank"
  },
  "timestamp": "2026-03-11T10:00:00Z"
}
```

### 2.2 현재 해석 가능한 주요 패턴

| 현재 형태 | 의미 | 클라이언트 해석 |
|---|---|---|
| `VALIDATION_FAILED` | 요청 검증 실패 | 입력 수정 |
| `ResponseStatusException` 기반 status + message | 서비스/업로드/S3 오류 | HTTP status + message 기준 해석 |
| `INTERNAL_SERVER_ERROR` | 처리되지 않은 예외 | 일반 장애 처리 |

### 2.3 운영 보조 헤더

- 요청/응답 헤더: `X-Correlation-Id`

즉 현재 Blog 서버는 **HTTP status와 message는 비교적 신뢰 가능하지만, `error.code` taxonomy는 아직 균일하지 않습니다.**

## 3. 왜 현재 모델이 단순한가

### 3.1 서비스 범위가 비교적 좁고, 구현도 아직 가벼운 편임

Blog 서버는 Auth/Report처럼 복잡한 상태 전이보다 아래에 집중합니다.

- 게시글 입력 검증
- 게시글 조회/수정/삭제
- 이미지 업로드 검증
- S3 업로드 예외 처리

그래서 초기에 `GlobalExceptionHandler + ResponseStatusException` 중심의 단순 모델로 시작한 것으로 볼 수 있습니다.

### 3.2 하지만 업로드/S3 오류는 이제 더 명확한 taxonomy가 필요함

현재는 아래가 주로 status + message로만 표현됩니다.

- unsupported content type
- file size exceeds limit
- s3 upload failed
- s3 client configuration error

즉 운영/클라이언트 관점에서는 이미 `VALIDATION_FAILED` 하나로는 부족한 상태입니다.

## 4. 현재 예외 매핑 방식

### 4.1 Validation

- `WebExchangeBindException` -> `400 VALIDATION_FAILED`
- 필드 에러는 `field: message` 형태로 이어 붙여 반환

### 4.2 ResponseStatusException

현재 여러 서비스 예외가 `ResponseStatusException`으로 직접 반환됩니다.

대표 예:

- 게시글 없음 -> `404`
- draft 상태 조회 규칙 위반 -> `400`
- unsupported content type -> `415`
- file size exceeds limit -> `413`
- s3 upload failed -> `502`
- s3 client configuration error -> `503`

### 4.3 Fallback

- 기타 예외 -> `500 INTERNAL_SERVER_ERROR`

## 5. 현재 모델에서 클라이언트가 주의할 점

### 5.1 `error.code`만으로는 충분하지 않을 수 있다

현재는 `VALIDATION_FAILED` 외에는 `ResponseStatusException` 기반 처리 비중이 커서, 클라이언트가 분기할 때 아래를 함께 봐야 합니다.

- HTTP status
- `error.message`

즉 현재 Blog 서버는 공통 표준처럼 `IMAGE_TOO_LARGE`, `IMAGE_UNSUPPORTED_FORMAT`, `FILE_UPLOAD_FAILED` 같은 분기 친화적 코드를 아직 제공하지 않습니다.

### 5.2 업로드 계열은 status 기반 해석이 우선이다

현재 기준으로는 아래처럼 해석하는 것이 현실적입니다.

- `413` -> 파일 용량 초과
- `415` -> 지원하지 않는 이미지 형식
- `502`/`503` -> S3/외부 의존성 장애

## 6. 공통 표준 대비 부족한 점

현재 Tech Blog 서버는 아래 점에서 공통 표준과 차이가 있습니다.

- `ErrorCode` enum 부재
- `ErrorResponseFactory` 부재
- `VALIDATION_FAILED` 등 코드명이 공통 표준과 다름
- 업로드/S3 오류를 공통 taxonomy로 정리하지 않음
- `X-Correlation-Id`는 있으나 에러 모델과의 결합이 약함

## 7. 향후 정렬 계획

### 7.1 1단계: 코드명 정렬

- `VALIDATION_FAILED` -> `VALIDATION_ERROR`
- 공통 `ErrorCode` enum 도입

### 7.2 2단계: 이미지/업로드 오류 분리

아래 코드는 Blog에서 우선순위가 높습니다.

- `IMAGE_TOO_LARGE`
- `IMAGE_UNSUPPORTED_FORMAT`
- `FILE_UPLOAD_FAILED`

### 7.3 3단계: 외부 의존성 오류 분리

S3/외부 스토리지 계열은 아래처럼 분리하는 것이 적절합니다.

- `EXTERNAL_API_FAILED`
- `SERVICE_UNAVAILABLE`

## 8. 구현 기준 파일

- `src/main/kotlin/com/aandiclub/tech/blog/common/error/GlobalExceptionHandler.kt`
- `src/main/kotlin/com/aandiclub/tech/blog/common/api/ApiResponse.kt`
- `src/main/kotlin/com/aandiclub/tech/blog/presentation/image/ImageUploadServiceImpl.kt`
- `src/main/kotlin/com/aandiclub/tech/blog/presentation/image/ImageUploadValidator.kt`

## 9. 결론

Tech Blog 서버의 현재 에러 모델은 **작고 단순한 서비스에 맞춘 초기 형태**입니다.

- 단순한 CRUD/업로드 흐름에는 동작하지만
- 공통 taxonomy 기준으로는 아직 세분화가 부족하고
- 특히 이미지/S3 오류는 더 명시적인 코드 체계가 필요합니다.

즉 Blog `Error-Model-v1`는 현재 구현을 설명하는 문서이면서, 동시에 **업로드/외부 연동 중심으로 다음 단계 정렬이 필요한 서비스**라는 점을 분명히 보여주는 문서입니다.
