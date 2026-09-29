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

## 파일명·클래스명 명명 규칙

파일명과 public 클래스명은 동일하게 유지하고, 여러 단어는 PascalCase로 작성합니다. 클래스 역할을 이름에 포함해 파일의 책임을 바로 알 수 있도록 합니다.

### 공통 규칙

- 도메인명과 기능명은 PascalCase로 작성합니다. 예: `AiDocument`, `CareRelation`
- 파일명은 클래스명과 동일하게 작성합니다. 예: `AiDocumentQueryService.java` 안의 클래스명은 `AiDocumentQueryService`
- 약어도 기존 프로젝트 표기인 `Ai`, `Dto`, `Id`, `Api`를 사용합니다. `AI`, `DTO`, `ID`, `API`처럼 전부 대문자로 쓰지 않습니다.
- 엔티티 파일은 `domain/entity` 폴더에 모아 두며, 클래스명 뒤에 일괄적으로 `Entity`를 붙이지 않습니다. 예: `MedicalDocument`, `AiExplanation`
- enum은 의미를 나타내는 도메인명 뒤에 `Status`, `Type`, `Role` 등 역할을 붙입니다. 예: `DocumentStatus`, `DocumentType`

### 계층별 suffix 규칙

| 계층 | 파일명 규칙 | 예시 |
|---|---|---|
| Controller | `{기능}Controller` 또는 `{기능}ApiController` | `AiDocumentQueryController`, `CareRelationApiController` |
| Request DTO | `{기능}RequestDto` | `AiDocumentListRequestDto` |
| Response DTO | `{기능}ResponseDto` | `AiDocumentDetailResponseDto` |
| Query Service | `{기능}QueryService` | `AiDocumentQueryService` |
| Command Service | `{기능}CommandService` | `AiDocumentCommandService` |
| Permission Service | `{기능}PermissionService` | `CareRelationPermissionService` |
| Mapper | `{기능}ResponseMapper` | `AiDocumentResponseMapper` |
| Query Model - 조회 행 | `{기능}Row` | `AiDocumentListRow` |
| Query Model - 조회 결과 | `{기능}View` | `AiDocumentDetailView` |
| Repository | `{기능}Repository` | `AiDocumentRepository` |
| Entity | 기능을 나타내는 명사 | `MedicalDocument`, `DocumentAnalysis` |
| Error Code | `{도메인}ErrorCode` | `AiDocumentErrorCode` |

### DTO 작성 규칙

- HTTP 입력 DTO는 `api/dto/request`에 `{기능}RequestDto`로 작성합니다.
- HTTP 출력 DTO는 `api/dto/response`에 `{기능}ResponseDto`로 작성합니다.
- DTO는 HTTP 계약을 표현하고, Entity를 직접 응답 타입으로 사용하지 않습니다.
- 조회 Service는 Response DTO가 아닌 `query/model`의 `Row` 또는 `View`를 반환하고, `api/mapper`의 Mapper가 Response DTO로 변환합니다.
- DTO 필드명은 lowerCamelCase를 사용합니다.

### 메서드명 규칙

- 조회: `find...`, `get...`, `require...`
- 생성: `create...`, `register...`
- 수정: `update...`, `change...`
- 삭제: `delete...`, `remove...`
- 권한 확인: `can...`, `has...`, `require...`
- 각 public 메서드 위에는 메서드의 목적과 반환 결과를 설명하는 한국어 주석을 작성합니다.

```text
{domain}/
├─ api/controller/         # HTTP Controller
├─ api/dto/request/        # HTTP 요청 DTO
├─ api/dto/response/       # HTTP 응답 DTO
├─ api/mapper/             # Query Model → HTTP Response DTO
├─ command/application/    # 상태 변경 유스케이스
├─ query/application/      # 읽기 전용 유스케이스
├─ query/model/            # API에 의존하지 않는 조회 모델
├─ domain/                 # Entity, Enum, 도메인 규칙
│  └─ repository/          # 도메인 Repository 인터페이스
├─ error/                  # 도메인별 ErrorCode
└─ infrastructure/         # 외부 시스템 구현체가 필요할 때만 생성
```

`user`, `care_relation`은 실제 구현이 있으며, 나머지 도메인도 위 구조 전체를 `package-info.java`로 미리 유지합니다. 아직 코드가 없는 계층도 기능을 시작할 공통 위치이므로 빈 패키지를 임의로 삭제하거나 별도 최상위 Controller·Service·Repository 폴더를 만들지 않습니다.

## Command와 Query

- `command/application`: 생성·수정·삭제·상태 전이와 권한 검사를 처리하고 `@Transactional`을 사용합니다.
- `query/application`: 상태를 바꾸지 않는 조회를 처리하고 `@Transactional(readOnly = true)`를 사용합니다.
- Query Service는 HTTP Response DTO 대신 `query/model`을 반환하고, `api/mapper`가 Response DTO로 변환합니다.
- Controller는 HTTP 요청·응답과 Application Service 호출만 담당합니다. 비즈니스 로직·직접 SQL·Entity 직접 응답을 넣지 않습니다.

현재는 Command와 Query가 하나의 PostgreSQL을 공유하는 점진적 CQRS입니다. 읽기 DB 분리·이벤트 소싱·메시지 브로커는 도입하지 않습니다.

## 새 기능 구현 순서

1. `{domain}/domain`에 Entity·Enum·도메인 규칙을, `{domain}/domain/repository`에 Repository 인터페이스를 둡니다.
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
