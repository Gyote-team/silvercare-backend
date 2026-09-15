# 🧭 백엔드 개발 규약

이 문서는 **코드를 어떻게 작성할지**에 대한 기준입니다. 패키지의 전체 위치는 [프로젝트 구조](project-structure.md)를 확인합니다.

## Command와 Query

- `command/application`: 생성·수정·삭제·상태 전이와 권한 검사를 처리하고 `@Transactional`을 사용합니다.
- `query/application`: 상태를 바꾸지 않는 조회를 처리하고 `@Transactional(readOnly = true)`를 사용합니다.
- Query Service는 HTTP Response DTO 대신 `query/model`을 반환하고, `api/mapper`가 Response DTO로 변환합니다.
- Controller는 HTTP 요청·응답과 Application Service 호출만 담당합니다. 비즈니스 로직·직접 SQL·Entity 직접 응답을 넣지 않습니다.

현재는 Command와 Query가 하나의 PostgreSQL을 공유하는 점진적 CQRS입니다. 읽기 DB 분리·이벤트 소싱·메시지 브로커는 도입하지 않습니다.

## 새 기능 구현 순서

1. `{domain}/domain`에 Entity·Enum·Repository·도메인 규칙을 둡니다.
2. DB 변경은 `src/main/resources/db/migration/V{번호}__{설명}.sql`로 추가합니다.
3. 상태 변경은 Command Service, 조회는 Query Service에 구현합니다.
4. HTTP Controller·DTO는 `api` 아래에 두고, 조회 모델 변환은 `api/mapper`에서 처리합니다.
5. 도메인 규칙 위반은 도메인의 ErrorCode와 `BusinessException`으로 표현합니다.

## 예외 처리

```text
{domain}ErrorCode
      ↓
BusinessException(ErrorCode)
      ↓
GlobalExceptionHandler
      ↓
공통 JSON 오류 응답
```

- 공통 계약: `global.exception.ErrorCode`
- 공통 예외: `global.exception.BusinessException`
- 공통 응답: `global.exception.ErrorResponse`
- 공통 처리: `global.exception.GlobalExceptionHandler`
- `UserErrorCode`, `CareRelationErrorCode`처럼 도메인별 오류는 `{domain}/error`에 둡니다.
- 여러 도메인에서 공통으로 쓰는 오류만 `global.exception`에 둡니다.
- Controller에서 예외 메시지 문자열을 해석하지 않습니다.
- Bean Validation 실패와 잘못된 JSON도 공통 오류 형식으로 처리합니다.

## DB 변경

- 적용된 Flyway migration은 수정하지 않습니다.
- 스키마 변경마다 새 migration을 추가하고, 관련 Entity·테스트를 같은 PR에 포함합니다.
- `.env`, OAuth·JWT 키, DB 덤프, 실제 의료 정보, 업로드 원본은 Git에 포함하지 않습니다.
