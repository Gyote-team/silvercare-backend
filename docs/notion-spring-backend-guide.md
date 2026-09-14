# 🩺 SilverCare Spring Backend 폴더 구조 및 팀 개발 가이드

> 문서 유형: Engineering / How-To  
> 대상: SilverCare Spring Backend 개발팀  
> 기준 브랜치: `develop`  
> 저장소: `silvercare-backend`

---

## 1. 이 문서의 목적

`silvercare-backend`는 사용자 인증, 권한, 동의, 건강 데이터, 일정과 AI 연동 경계를 담당하는 Spring Boot 저장소입니다.

이 문서는 팀원이 다음 내용을 같은 기준으로 이해하도록 돕습니다.

- 새 기능을 어느 도메인 폴더에서 개발하는지
- Controller, DTO, Command, Query, Entity, Repository의 위치와 역할
- DB 변경과 예외 처리를 어떻게 하는지
- Python AI 서버와 어떤 경계에서 협업하는지
- PR 전에 무엇을 확인해야 하는지

빈 패키지는 기능이 시작될 위치를 보여 주기 위한 공통 뼈대입니다. 실제 구현 파일은 담당 기능 개발 시 추가합니다.

---

## 2. 전체 시스템에서 Spring의 위치

```text
[사용자 / 보호자]
        │
        ▼
[Next.js 화면]
        │ 공개 API /api/*
        ▼
[Spring Boot]
  ├─ 카카오 로그인 · JWT · 권한
  ├─ 사용자 · 보호자 연결 · 열람 동의
  ├─ 건강기록 · 방문 · 문서 · 일정 데이터 관리
  ├─ PostgreSQL · Flyway migration
  └─ AI 요청 전 권한 확인 및 허용 식별자 전달
        │ 내부 AI API (추후)
        ▼
[Python AI Server]
  └─ OCR · STT · 문서 추출 · RAG · AI 응답 생성
```

### 가장 중요한 원칙

사용자 화면은 Python AI 서버를 직접 호출하지 않습니다.

```text
올바른 흐름
Next.js → Spring Boot → Python AI

금지된 흐름
Next.js → Python AI
```

Spring은 “현재 사용자가 이 데이터에 접근해도 되는가?”를 판단하고, Python은 Spring이 허용한 데이터만 처리합니다.

---

## 3. 전체 폴더 구조

```text
src/main/java/com/gyote/silvercare/
├─ global/
│  ├─ auth/                 # OAuth2, JWT, 쿠키, 인증 필터
│  ├─ config/               # Security, CORS, 환경 설정
│  ├─ exception/            # 공통 ErrorCode, BusinessException, Handler
│  └─ web/                  # 공통 웹 진입점
│
├─ user/                    # 회원·역할
├─ care_relation/           # 개인-보호자 연결
├─ consent/                 # 보호자 열람 동의
├─ health_record/           # 건강기록
├─ visit/                   # 병원 방문
├─ medical_document/        # 의료 문서 메타데이터·업로드
├─ action_item/             # 할 일 후보
├─ schedule/                # 일정·복약 알림
├─ timeline/                # 통합 타임라인 조회
└─ voice_recording/         # 건강기록 음성·진료실 녹음

src/main/resources/
├─ application.yml
└─ db/migration/            # Flyway SQL migration
```

---

## 4. 도메인 폴더를 이해하는 방법

새 기능은 최상위 공용 Controller·Service 폴더가 아니라, 반드시 해당 도메인 내부에 추가합니다.

```text
{domain}/
├─ api/
│  ├─ controller/           # HTTP Endpoint
│  ├─ dto/request/          # HTTP 요청 DTO
│  ├─ dto/response/         # HTTP 응답 DTO
│  └─ mapper/               # Query Model → Response DTO
├─ command/application/     # 생성·수정·삭제·상태 변경
├─ query/
│  ├─ application/          # 읽기 전용 조회
│  └─ model/                # API에 의존하지 않는 조회 모델
├─ domain/
│  └─ repository/           # Entity, Enum, Repository, 도메인 규칙
└─ error/                   # 도메인별 ErrorCode
```

요청의 흐름은 다음과 같습니다.

```text
Controller
   ↓
Command Service 또는 Query Service
   ↓
Domain Entity · Repository
   ↓
PostgreSQL
```

Query 결과는 HTTP DTO를 직접 만들지 않습니다.

```text
Query Service → Query Model → API Mapper → Response DTO
```

화면 응답 형식이 바뀌어도 조회 로직을 재사용할 수 있도록 하기 위함입니다.

---

## 5. 계층별 역할과 금지 사항

### `api/controller`

- URL, HTTP Method, 인증 사용자, 요청 DTO, 응답 상태만 처리합니다.
- Command 또는 Query Service를 호출합니다.
- 긴 비즈니스 로직, 직접 SQL, Entity 직접 응답은 넣지 않습니다.

### `api/dto`

- 외부 HTTP 요청·응답 형식을 정의합니다.
- Entity를 화면에 직접 노출하지 않습니다.

### `command/application`

- 생성·수정·삭제·상태 전이를 처리합니다.
- `@Transactional`을 사용합니다.
- 예: 역할 선택, 보호자 연결 요청·수락·거절, 동의 철회

### `query/application`

- 목록·상세·타임라인처럼 읽기만 하는 기능을 처리합니다.
- `@Transactional(readOnly = true)`를 사용합니다.
- API Response DTO가 아닌 Query Model을 반환합니다.

### `domain`

- Entity, Enum, Value Object, Repository, 도메인 규칙을 둡니다.
- HTTP·Controller·화면 DTO에 의존하지 않습니다.

### `error`

- 해당 도메인에서만 쓰는 ErrorCode enum을 둡니다.
- 예: `UserErrorCode`, `CareRelationErrorCode`

---

## 6. DB와 JPA 기준

JPA는 Entity 매핑과 실제 데이터 CRUD에 사용합니다. 테이블·컬럼 구조 변경은 Flyway SQL migration으로 관리합니다.

```text
src/main/resources/db/migration/
├─ V1__create_users_and_care_relations.sql
└─ V2__create_health_records.sql
```

### 변경 규칙

- 적용된 Migration 파일은 수정하지 않습니다.
- 스키마 변경마다 새 `V번호__설명.sql` 파일을 추가합니다.
- Entity를 바꿀 때는 Migration과 테스트를 같은 PR에 포함합니다.
- `.env`, DB 덤프, 실제 의료 정보는 Git에 올리지 않습니다.

---

## 7. 예외 처리 기준

```text
{domain}ErrorCode
      ↓
BusinessException
      ↓
GlobalExceptionHandler
      ↓
공통 JSON 오류 응답
```

예시:

```java
throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
```

```json
{
  "code": "USER_003",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": "..."
}
```

- Controller에서 예외 메시지 문자열을 직접 해석하지 않습니다.
- 잘못된 요청 DTO와 JSON도 공통 오류 형식으로 처리합니다.
- 여러 도메인이 공통으로 쓰는 오류만 `global/exception`에 둡니다.

---

## 8. Spring과 Python AI의 협업 경계

### Spring 담당

- 사용자 인증·권한
- 개인-보호자 연결과 열람 동의
- AI 요청 대상 데이터의 접근 가능 여부 판단
- 문서·음성 원본의 메타데이터와 저장 위치 관리
- AI 처리 요청의 식별자 전달과 결과 공개 전 재검증

### Python 담당

- OCR, STT, 문서 구조화
- 임베딩, RAG, AI 답변 생성
- 분석 결과와 처리 상태 반환

### 함께 합의해야 하는 것

- Spring 요청 DTO와 Python Pydantic 모델
- 내부 API URL·Method·오류 코드
- `requestId`, `patientId`, 허용 문서 ID 등 전달 규칙
- callback·처리 상태·재시도 규칙

계약을 바꾸는 PR은 Spring과 Python 담당자가 함께 확인합니다.

---

## 9. 테스트와 PR 규칙

```powershell
.\mvnw.cmd test
```

PR 전 확인:

- 테스트 통과
- Flyway migration 포함 여부
- `.env`·API Key·JWT Secret 미포함
- Controller에 비즈니스 로직이 없는지
- Command와 Query 책임이 섞이지 않았는지
- ErrorCode와 예외 응답 테스트가 있는지

### 브랜치 규칙

```text
feat/health-record → develop → main
```

- `develop`, `main` 직접 push 금지
- 기능 브랜치에서 작업
- PR은 `develop` 대상으로 생성
- 최소 1명 승인 후 병합

---

## 10. 현재 구현 상태

### 구현됨

- 카카오 로그인
- JWT·세션 인증
- 역할 선택
- 개인-보호자 연결
- 공통 예외 처리
- PostgreSQL·Flyway 기반 스키마 관리

### 구조 준비됨

- 동의, 건강기록, 병원 방문, 의료 문서
- 할 일 후보, 일정·복약 알림, 타임라인, 음성 녹음

### 별도 저장소

- OCR, STT, LLM, RAG 등 Python AI 처리
