# 🏛️ SilverCare AI · 아키텍처와 데이터 운영

## 🎯 핵심 결정

- Spring Boot는 인증·권한·도메인 API·데이터 정합성을 담당합니다.
- Python AI 서버는 OCR·STT·문서 추출·RAG·LLM 처리를 담당하며 **별도 레포**로 운영합니다.
- 업무 데이터와 문서 검색용 임베딩은 PostgreSQL + pgvector 한 인스턴스에서 관리합니다.
- CQRS는 DB를 나누지 않고, 코드의 Command와 Query 책임부터 분리하는 방식으로 시작합니다.

## 🗺️ 구성도

```text
Next.js Web / WebView
        │ HTTPS
        ▼
Spring Boot
  ├─ 인증 · 권한 · 동의 · 도메인 API
  ├─ PostgreSQL + pgvector     업무 데이터 · 벡터
  └─ Object Storage            의료 서류 원본

Python AI Server (별도 레포)
  └─ OCR · STT · 정보 추출 · RAG · 근거 검증 · LLM
```

브라우저는 Python AI 서버를 직접 호출하지 않습니다. 추후 AI 연동 시 Spring이 인증·연결·동의를 검증하고 허용된 식별자만 전달합니다.

## 🧩 CQRS 규칙

```text
{domain}
├─ api/controller              Controller
├─ api/dto/request             요청 DTO
├─ api/dto/response            응답 DTO
├─ api/mapper                  Query Model → HTTP Response DTO
├─ command/application         상태 변경 유스케이스와 트랜잭션
├─ query/application           읽기 전용 조회
├─ query/model                 API 계층에 의존하지 않는 조회 모델
├─ domain                      Entity, Enum, Repository, 도메인 정책
└─ error                       도메인별 ErrorCode
```

- **Command**: 생성·수정·삭제·상태 전이와 권한 검사를 담당합니다.
- **Query**: 상태를 바꾸지 않고 화면에 필요한 데이터를 조립합니다.
- Query Service는 HTTP Response DTO에 의존하지 않습니다. `query/model`을 반환하고 `api/mapper`가 HTTP 응답으로 변환합니다.
- Command와 Query는 현재 하나의 PostgreSQL을 공유합니다.
- 읽기 DB 복제, 이벤트 소싱, Kafka는 현재 도입하지 않습니다.

적용 예시는 다음과 같습니다.

- `user.command.application.UserAccountService` / `user.query.application.UserQueryService`
- `care_relation.command.application.CareRelationCommandService` / `care_relation.query.application.CareRelationQueryService`

## 📦 Spring 범위

### 구현됨

- `user`: 카카오 로그인 사용자와 역할
- `care_relation`: 개인-보호자 연결 요청과 상태 전이
- `global`: 인증·보안·설정·예외 처리

### 패키지 뼈대만 준비됨

- `consent`, `health_record`, `visit`, `medical_document`
- `action_item`, `schedule`, `timeline`, `voice_recording`

음성 녹음은 서비스 범위에 포함됩니다. Spring은 파일·권한·메타데이터를 관리하고, STT 변환은 별도 Python AI 서버가 맡습니다.

## 🗄️ 데이터와 Git 원칙

- Flyway migration, ERD, API 문서, `.env.example`, Docker Compose는 Git에 포함합니다.
- `.env`, OAuth·JWT 키, DB 덤프, 실제 의료 정보, 업로드 원본은 Git에 포함하지 않습니다.
- 이미 적용한 `V1__...sql`은 수정하지 않고, 변경마다 새 migration을 추가합니다.
- 의료 문서 원본은 DB BLOB가 아닌 Object Storage 키와 메타데이터만 저장합니다.
