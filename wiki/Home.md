# A&I TECH BLOG SERVER Wiki

## 문서 목록

- [PRD](PRD)
- [ERD](ERD)
- [API-Spec-v1](API-Spec-v1)
- [Error-Model-v1](Error-Model-v1)

## 고정 규칙

- Gateway 진입점은 `A-AND-I-GATEWAY-SERVER`가 담당
- 인증/인가 정책은 `A-AND-I-REPORT-AUTH-SERVER`(인증 서버)에서 관리
- 본 서버는 블로그 도메인(API: 게시글/이미지)만 담당
- 운영 인프라(AWS/EC2/배포)는 관리자 전담, 개발자는 인프라 변경 금지
- 개발 범위는 API 구현 및 로컬 검증 중심
- 개인 작업 메모/태스크는 `secret_docs/`에만 작성하며 Git 추적 금지

## 최근 변경 (2026-03-05)

- 위키 구조를 `Home + API-Spec-v1 + ERD + PRD` 4페이지로 정리
- `Home`에 문서 목록/고정 규칙/최근 변경 섹션 통합
- API 스펙을 코드베이스 전체 기준으로 재작성(Request/Response/에러 포함)
- ERD/PRD 문서 템플릿을 블로그 서버 기준으로 정비
