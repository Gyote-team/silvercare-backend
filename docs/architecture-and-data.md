# 🏛️ SilverCare AI · 아키텍처와 데이터 운영

이 문서는 **시스템 경계와 데이터 원칙**을 관리합니다. 코드 파일 위치와 구현 규칙은 [개발 규약](development-convention.md)을 따릅니다.

## 핵심 결정

- Spring Boot는 인증·권한·도메인 API·데이터 정합성을 담당합니다.
- Python AI 서버는 OCR·STT·문서 추출·RAG·LLM 처리를 담당하며 별도 레포로 운영합니다.
- 업무 데이터와 문서 검색용 임베딩은 PostgreSQL + pgvector 한 인스턴스에서 관리합니다.
- CQRS는 DB를 나누지 않고 Command와 Query의 코드 책임을 분리하는 방식으로 시작합니다.

## 시스템 경계

```text
Next.js Web / WebView
        │ HTTPS
        ▼
Spring Boot
  ├─ 인증 · 개인-보호자 연결 권한 · 도메인 API
  ├─ PostgreSQL + pgvector     업무 데이터 · 벡터
  └─ Object Storage            의료 서류 원본

Python AI Server (별도 레포)
  └─ OCR · STT · 정보 추출 · RAG · 근거 검증 · LLM
```

브라우저는 Python AI 서버를 직접 호출하지 않습니다. AI 요청 전 Spring이 인증과 개인-보호자 연결을 검증하고, 허용된 식별자만 Python에 전달합니다.

## 권한과 데이터

- 개인은 자신의 데이터에 접근할 수 있습니다.
- 보호자는 대상 개인과의 `care_relation.status == ACTIVE`일 때만 해당 개인의 전체 문서·정보를 열람할 수 있습니다.
- `REQUESTED`, `REJECTED`, `CANCELED`, `REVOKED` 관계는 보호자 열람 권한을 부여하지 않습니다.
- 기능별 열람 동의나 `consent` 도메인은 사용하지 않습니다. 쓰기 권한은 각 기능의 유스케이스 규칙으로 판단합니다.
- 의료 문서 원본은 DB BLOB가 아닌 Object Storage 키와 메타데이터만 저장합니다.

## 데이터와 Git 원칙

- Flyway migration, ERD, API 문서, `.env.example`, Docker Compose는 Git에 포함합니다.
- `.env`, OAuth·JWT 키, DB 덤프, 실제 의료 정보, 업로드 원본은 Git에 포함하지 않습니다.
