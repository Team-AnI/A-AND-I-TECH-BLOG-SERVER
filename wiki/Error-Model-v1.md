# Error-Model-v1

Tech Blog 서버의 현재 예외처리 모델 설명 문서입니다.

## 0. 문서 범위

- 이 문서는 **현재 구현 기준** Tech Blog 서버의 에러 응답 구조와 예외 매핑 상태를 설명합니다.
- 공통 백엔드 예외 표준은 Team-AnI `.github` 문서의 `Backend API Common Exception Handling Guide v1`를 따릅니다.
- Blog 특화 상세는 이 문서에서 관리합니다.

## 1. 현재 응답 Envelope

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

## 2. 현재 예외 매핑 방식

현재는 `GlobalExceptionHandler` 중심의 단순 모델입니다.

### 2.1 Validation

- `WebExchangeBindException` -> `400 VALIDATION_FAILED`

특징:

- 필드 에러를 `field: message` 형태로 이어 붙여 반환

### 2.2 ResponseStatusException

- `ResponseStatusException` -> status 그대로 응답
- `error.code`는 상태 이름 또는 숫자 문자열 사용

즉 현재는 `NOT_FOUND`, `BAD_REQUEST`, `UNSUPPORTED_MEDIA_TYPE` 등이
상태 기반으로 유동적으로 결정됩니다.

### 2.3 Fallback

- 기타 예외 -> `500 INTERNAL_SERVER_ERROR`

## 3. Blog 특화 시나리오

API 문서 기준으로 아래 케이스가 존재합니다.

- 게시글 입력 검증 실패
- 게시글 없음 / 삭제됨
- 이미지 업로드 Content-Type 오류
- 이미지 업로드 용량 초과
- S3 업로드 실패 / 클라이언트 오류

다만 현재 코드 기준으로는 이 시나리오들이 공통 `ErrorCode` enum으로 정리되어 있지 않습니다.

## 4. 현재 모델의 한계

현재 Tech Blog 서버는 아래 점에서 공통 표준 대비 부족합니다.

- `ErrorCode` enum 부재
- `ErrorResponseFactory` 부재
- `VALIDATION_FAILED` 등 코드명이 공통 표준과 다름
- 이미지/S3 오류를 공통 taxonomy로 정리하지 않음
- `X-Request-Id`/`X-Correlation-Id`와 에러 모델 연결이 약함

## 5. 권장 정렬 방향

공통 백엔드 표준에 맞출 때는 아래 순서가 적절합니다.

1. `VALIDATION_FAILED` -> `VALIDATION_ERROR` 정렬
2. 공통 `ErrorCode` enum 도입
3. 이미지/업로드 전용 코드 분리
   - `FILE_UPLOAD_FAILED`
   - `IMAGE_TOO_LARGE`
   - `IMAGE_UNSUPPORTED_FORMAT`
4. 외부 의존성 오류 분리
   - `EXTERNAL_API_FAILED`
   - `SERVICE_UNAVAILABLE`

## 6. 구현 기준 파일

- `src/main/kotlin/com/aandiclub/tech/blog/common/error/GlobalExceptionHandler.kt`
- `src/main/kotlin/com/aandiclub/tech/blog/common/api/ApiResponse.kt`
