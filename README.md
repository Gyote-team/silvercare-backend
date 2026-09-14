# 🩺 SilverCare AI · Spring Backend

고령 사용자와 보호자가 건강기록·병원 방문·의료 서류를 관리할 수 있도록 지원하는 Spring Boot 백엔드입니다. 화면은 [`../frontend`](../frontend)(Next.js)가 담당합니다.

> 🔐 비밀값은 코드나 Git에 넣지 않습니다. `.env.example`을 복사해 `.env`를 만들고 각 값을 채워 주세요.

---

## 🧭 빠른 안내

- **현재 구현됨**: 카카오 로그인, JWT/세션 인증, 최초 역할 선택, 개인-보호자 연결 관리
- **구조만 준비됨**: 동의, 건강기록, 방문, 문서, 할 일·일정·복약 알림, 타임라인, 음성 녹음
- **별도 레포**: OCR, STT, LLM, RAG 등 Python AI 서버

---

## 🏗️ 패키지 설계

기능(도메인)을 먼저 나누고, 도메인 내부에서 **상태 변경(Command)** 과 **읽기(Query)** 를 분리하는 점진적 CQRS 구조입니다.

전체 파일·패키지 트리와 책임은 [폴더 구조 문서](docs/project-structure.md)에서 확인할 수 있습니다.
새 기능의 파일 배치와 예외 처리 기준은 [개발 규약](docs/development-convention.md)을 따릅니다.
처음 참여하는 팀원을 위한 실행·개발·PR 안내는 [팀 개발 가이드](docs/team-development-guide.md)를 확인하세요.
회의에서 구조를 설명할 때는 [구조 설명 대본](docs/meeting-script.md)을 참고하세요.
Notion에 붙여 넣을 상세 팀 운영 문서는 [Notion용 백엔드 가이드](docs/notion-spring-backend-guide.md)를 참고하세요.

```text
com.gyote.silvercare
├─ global                         # 전 도메인이 공유하는 기술 영역
│  ├─ auth                        # OAuth2, JWT, 쿠키, Security Filter
│  ├─ config                      # Security, CORS 등 Spring 설정
│  ├─ exception                   # 공통 API 오류 처리
│  └─ web                         # 특정 도메인에 속하지 않는 웹 진입점
│
├─ user                           # 회원과 역할
│  ├─ api/controller              # HTTP Controller
│  ├─ api/dto/request             # HTTP 요청 DTO
│  ├─ api/dto/response            # HTTP 응답 DTO
│  ├─ api/mapper                  # Query Model → HTTP Response DTO
│  ├─ command/application         # 계정 생성, 역할 선택
│  ├─ query/application           # 로그인 사용자, 내 정보 조회
│  ├─ domain/repository           # User Entity와 저장소
│  └─ error                       # UserErrorCode
│
└─ care_relation                  # 개인-보호자 연결
   ├─ api/controller              # 연결 관리 API
   ├─ api/dto/request             # 연결 요청 DTO
   ├─ api/dto/response            # 연결 응답 DTO
   ├─ api/mapper                  # Query Model → HTTP Response DTO
   ├─ command/application         # 요청·수락·거절·취소·해제
   ├─ query/application           # 연결 목록 조회
   ├─ query/model                 # API에 의존하지 않는 조회 모델
   ├─ domain/repository           # CareRelation Entity와 저장소
   └─ error                       # CareRelationErrorCode
```
---

### ✍️ 도메인 공통 규칙

```text
{domain}
├─ api/controller              HTTP 진입점
├─ api/dto/request             HTTP 요청 DTO
├─ api/dto/response            HTTP 응답 DTO
├─ api/mapper                  Query Model을 HTTP 응답 DTO로 변환
├─ command/application         생성·수정·삭제·상태 전이, 트랜잭션
├─ query/application           읽기 전용 조회
├─ query/model                 API에 의존하지 않는 조회 모델
├─ domain                      Entity, Enum, Repository, 도메인 규칙
└─ error                       도메인별 ErrorCode
```

- **Command**는 데이터를 바꾸며, 권한 검사와 상태 전이를 처리합니다.
- **Query**는 데이터를 바꾸지 않고, 화면에 필요한 형태로 읽어 반환합니다.
- Query는 HTTP DTO가 아닌 `query/model`을 반환하고, `api/mapper`가 HTTP 응답으로 변환합니다.
- 현재는 Command와 Query가 같은 PostgreSQL을 공유합니다. 읽기 DB 분리·이벤트 소싱·메시지 브로커는 도입하지 않습니다.

---

## 🧩 준비된 도메인 뼈대

구현 예정 패키지는 `package-info.java`로 책임을 기록해 두었습니다. 실제 개발을 시작하면 해당 도메인 안에 DTO·Repository·Entity를 추가합니다.

- `consent` — 보호자 열람 동의
- `health_record` — 건강기록
- `visit` — 병원 방문
- `medical_document` — 의료 문서 메타데이터와 업로드
- `action_item` — 문서에서 생성된 할 일 후보의 승인·거절
- `schedule` — 일정·복약 알림
- `timeline` — 여러 도메인을 한 화면으로 조립하는 조회 전용 모델
- `voice_recording` — 건강기록 음성·진료실 녹음의 업로드와 메타데이터

> 🤖 `integration/ai`, `chat`, `comparison` 같은 AI 처리 패키지는 의도적으로 없습니다. Spring은 인증·권한·동의·업로드 메타데이터를 맡고, OCR·STT·LLM 처리는 별도 Python AI 서버가 맡습니다.

---

## 🔐 인증과 권한

- 카카오 로그인 뒤 세션 쿠키 `SILVERCARE_SESSION`과 JWT 쿠키 `SILVERCARE_TOKEN`을 발급합니다.
- API는 `Authorization: Bearer` 헤더 또는 JWT 쿠키로 인증합니다.
- 보호자만 연결 요청을 할 수 있고, 개인만 연결 요청을 수락·거절할 수 있습니다.
- 도메인 오류는 `ErrorCode → BusinessException → GlobalExceptionHandler`로 동일한 JSON 형식으로 반환합니다.

---

## 🗄️ 데이터베이스

- DB는 **PostgreSQL + pgvector** 단일 인스턴스를 사용합니다.
- 스키마 변경은 Flyway migration으로 관리합니다.
- 실제 DB 데이터, 덤프, `.env`, OAuth·JWT 키, 실제 의료 정보는 Git에 올리지 않습니다.

자세한 데이터·CQRS 운영 원칙은 [아키텍처 문서](docs/architecture-and-data.md)를 확인하세요.

---

## 🚀 실행

```powershell
cd backend
docker compose up -d
.\run.ps1
```

- API 및 카카오 콜백: `http://localhost:8080`
- 웹 화면: `http://localhost:3000`

---------------

## 🧪 테스트

```powershell
.\mvnw.cmd clean test
```

현재 테스트는 사용자·보호자 연결·보안 설정·공통 예외 응답을 포함합니다.
