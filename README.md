# 🩺 SilverCare AI · Spring Backend

고령 사용자와 보호자의 건강기록·병원 방문·의료 서류를 지원하는 Spring Boot 백엔드입니다. 화면은 [`../frontend`](../frontend)(Next.js)가 담당합니다.

> 🔐 비밀값은 코드나 Git에 넣지 않습니다. `.env.example`을 복사해 `.env`를 만들고 각 값을 채워 주세요.

## 현재 범위

- **구현됨**: 카카오 로그인, JWT/세션 인증, 최초 역할 선택, 개인-보호자 연결 관리
- **구조 준비됨**: 건강기록, 방문, 문서, 할 일·일정·복약 알림, 타임라인, 음성 녹음
- **별도 레포**: OCR, STT, LLM, RAG 등 Python AI 서버

## 문서 안내

문서마다 한 가지 기준만 관리합니다.

- [아키텍처와 데이터 운영](docs/architecture-and-data.md): Spring·Python 경계, CQRS 적용 범위, 데이터·권한 원칙
- [프로젝트 구조](docs/project-structure.md): 실제 폴더·패키지 트리와 파일 위치
- [개발 규약](docs/development-convention.md): Command·Query, 예외 처리, DB 변경 규칙
- [팀 개발 가이드](docs/team-development-guide.md): 실행, 브랜치, PR, 테스트 절차

## 권한 원칙

- 개인은 자신의 데이터를 열람할 수 있습니다.
- 보호자는 대상 개인과의 연결이 `ACTIVE`일 때만 그 개인의 전체 문서·정보를 열람할 수 있습니다.
- `REQUESTED`, `REJECTED`, `CANCELED`, `REVOKED` 상태에서는 보호자 열람을 허용하지 않습니다.
- 별도 열람 동의 도메인은 두지 않으며, 쓰기 권한은 각 기능의 유스케이스에서 판단합니다.

## 실행

```powershell
cd backend
docker compose up -d
.\run.ps1
```

- API 및 카카오 콜백: `http://localhost:8080`
- 웹 화면: `http://localhost:3000`

## 테스트

```powershell
.\mvnw.cmd clean test
```

현재 테스트는 사용자·보호자 연결·보안 설정·공통 예외 응답을 포함합니다.
