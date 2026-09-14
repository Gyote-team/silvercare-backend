# 🧭 백엔드 개발 패키지·예외 처리 규약

새 기능은 반드시 **도메인 폴더 하나** 안에서 구현합니다. 최상위에 Controller·Service·Repository를 기능과 무관하게 모으지 않습니다.

```text
{domain}/
├─ api/
│  ├─ controller/          # HTTP Endpoint. Command·Query application만 호출
│  ├─ dto/
│     ├─ request/          # HTTP 요청 DTO
│     └─ response/         # HTTP 응답 DTO
│  └─ mapper/              # Application 조회 모델을 HTTP DTO로 변환
├─ command/
│  └─ application/         # 생성·수정·삭제·상태 전이. @Transactional
├─ query/
│  ├─ application/         # 읽기 전용 조회. @Transactional(readOnly = true)
│  └─ model/               # API 계층에 의존하지 않는 읽기 전용 모델
├─ domain/
│  ├─ repository/          # Repository 인터페이스
│  └─ ...                  # Entity, Enum, Value Object, 도메인 규칙
├─ error/                  # 도메인별 ErrorCode enum
└─ infrastructure/         # 외부 시스템·파일 저장소 등 구현체가 필요할 때만 생성
```

## 새 기능 추가 순서

1. `{domain}/domain`에 Entity·Enum·Repository를 둡니다.
2. DB 구조 변경은 `src/main/resources/db/migration/V{번호}__{설명}.sql`로 추가합니다.
3. 상태 변경은 `command/application` Service에, 조회는 `query/application` Service에 구현합니다.
4. HTTP Controller·DTO는 `api` 아래에 두고, Query Model → HTTP DTO 변환은 `api/mapper`에서 처리합니다.
5. 도메인 규칙 위반은 `{domain}/error`의 ErrorCode와 `BusinessException`으로 표현합니다.
6. 기능 브랜치에서 테스트 후 PR을 만듭니다.

## 예외 처리 규약

```text
{domain}/error/{Domain}ErrorCode  # 도메인별 오류 코드·HTTP 상태·메시지
                 ↓
BusinessException(ErrorCode)      # Service에서 발생
                 ↓
GlobalExceptionHandler            # 공통 API 오류 응답으로 변환
```

- 공통 계약: `global.exception.ErrorCode`
- 공통 예외: `global.exception.BusinessException`
- 공통 응답: `global.exception.ErrorResponse`
- 공통 처리: `global.exception.GlobalExceptionHandler`
- `IllegalArgumentException`의 메시지 문자열을 Controller에서 해석하지 않습니다.
- Bean Validation 실패와 잘못된 JSON 요청도 `GlobalExceptionHandler`가 공통 오류 응답으로 처리합니다.
- `UserErrorCode`, `CareRelationErrorCode`처럼 도메인별 `error` 패키지에 enum을 추가합니다.
- 여러 도메인에서 공통으로 쓰는 오류만 `global.exception`에 둡니다.

## 현재 구현 범위

회원(`user`)과 개인-보호자 연결(`care_relation`)은 실제 Controller·DTO도 위 위치로 정리되어 있습니다. 나머지 도메인은 기능 시작 시 이 규약의 빈 패키지를 채워 구현합니다.
