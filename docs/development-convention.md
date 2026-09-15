# 🧭 백엔드 개발 규약

이 문서는 **패키지 구조와 코드 작성 방식의 단일 기준**입니다.

## 패키지 구조와 파일 배치

```text
src/main/java/com/gyote/silvercare/
├─ global/                           # 인증·설정·공통 예외·공통 웹 진입점
├─ user/                             # 회원·역할
├─ care_relation/                    # 개인-보호자 연결
├─ health_record/                    # 건강기록 (구조 준비)
├─ visit/                            # 병원 방문 (구조 준비)
├─ medical_document/                 # 의료 문서·업로드 메타데이터 (구조 준비)
├─ action_item/                      # 할 일 후보 승인·거절 (구조 준비)
├─ schedule/                         # 일정·복약 알림 (구조 준비)
├─ timeline/                         # 통합 타임라인 조회 (구조 준비)
└─ voice_recording/                  # 건강기록·진료실 녹음 메타데이터 (구조 준비)
```

```text
{domain}/
├─ api/controller/         # HTTP Controller
├─ api/dto/request/        # HTTP 요청 DTO
├─ api/dto/response/       # HTTP 응답 DTO
├─ api/mapper/             # Query Model → HTTP Response DTO
├─ command/application/    # 상태 변경 유스케이스
├─ query/application/      # 읽기 전용 유스케이스
├─ query/model/            # API에 의존하지 않는 조회 모델
├─ domain/                 # Entity, Enum, Repository, 도메인 규칙
├─ error/                  # 도메인별 ErrorCode
└─ infrastructure/         # 외부 시스템 구현체가 필요할 때만 생성
```

`user`, `care_relation`은 실제 구현이 있으며, 나머지 도메인의 빈 패키지는 기능을 시작할 공통 위치입니다. 빈 패키지를 임의로 삭제하거나 별도 최상위 Controller·Service·Repository 폴더를 만들지 않습니다.

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
