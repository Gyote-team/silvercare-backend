# 👋 SilverCare 백엔드 팀 개발 가이드

이 문서만 보면 새 기능을 어디에 만들고, 어떤 순서로 PR을 올려야 하는지 알 수 있습니다.

## 1. 시작 전

```powershell
cd backend
git switch develop
git pull origin develop
git switch -c feat/health-record
docker compose up -d
Copy-Item .env.example .env
.\run.ps1
```

- `.env`에는 카카오·JWT·DB 비밀값을 넣습니다. Git에 절대 커밋하지 않습니다.
- DB는 Docker PostgreSQL 컨테이너를 사용합니다. Spring 서버는 IDE 또는 `run.ps1`로 실행합니다.
- `develop`, `main`에는 직접 커밋하거나 푸시하지 않습니다.

## 2. 새 기능 파일 위치

건강기록을 예로 들면 다음과 같이 추가합니다.

```text
health_record/
├─ api/controller/                 # HealthRecordController
├─ api/dto/request/                # CreateHealthRecordRequest
├─ api/dto/response/               # HealthRecordResponse
├─ api/mapper/                     # 조회 모델을 HTTP 응답 DTO로 변환
├─ command/application/            # HealthRecordCommandService
├─ query/application/              # HealthRecordQueryService
├─ query/model/                    # API 계층에 의존하지 않는 조회 모델
├─ domain/                         # HealthRecord Entity, Enum, Value Object
├─ domain/repository/              # HealthRecordRepository
└─ error/                          # HealthRecordErrorCode
```

- **Controller**: HTTP 요청·응답 처리만 담당합니다.
- **Command Service**: 생성·수정·삭제·상태 변경입니다.
- **Query Service**: 목록·상세처럼 데이터를 읽기만 하는 기능입니다.
- **Repository**: DB 조회·저장 인터페이스입니다.
- **DTO**: Controller 밖으로 Entity를 직접 노출하지 않기 위한 요청·응답 객체입니다.

## 3. DB 변경

테이블과 컬럼 변경은 JPA 자동 생성이 아니라 Flyway SQL로 관리합니다.

```text
src/main/resources/db/migration/
└─ V2__create_health_records.sql
```

Entity는 Java 코드에서 테이블을 다루기 위한 매핑이고, 실제 DB 구조 변경 이력은 SQL migration이 담당합니다.

## 4. 예외 처리

도메인 규칙 위반에는 문자열 예외 대신 ErrorCode를 사용합니다.

```java
throw new BusinessException(HealthRecordErrorCode.RECORD_NOT_FOUND);
```

`GlobalExceptionHandler`가 이를 다음처럼 동일한 JSON 형식으로 반환합니다.

```json
{
  "code": "HEALTH_RECORD_001",
  "message": "건강기록을 찾을 수 없습니다.",
  "timestamp": "..."
}
```

공통 오류는 `global/exception`, 도메인 규칙 오류는 `{domain}/error`에 둡니다.

## 5. 보호자 열람 권한

- 개인은 자신의 건강기록·방문·문서 데이터를 열람할 수 있습니다.
- 보호자는 대상 개인과의 `care_relation`이 `ACTIVE`일 때만 그 개인의 전체 문서·정보를 열람할 수 있습니다.
- 연결이 `REQUESTED`, `REJECTED`, `CANCELED`, `REVOKED`이면 보호자 열람은 허용하지 않습니다.
- 기능별 열람 동의는 만들지 않습니다. 쓰기·수정 권한은 각 도메인의 유스케이스 규칙으로 별도 제한합니다.

새 도메인의 조회 API를 만들 때는 개인 본인인지, 또는 보호자라면 `ACTIVE` 연결인지 먼저 검사합니다.

## 6. PR 절차

```powershell
git add .
git commit -m "feat: add health record creation"
git push -u origin feat/health-record
```

GitHub에서 `feat/health-record` → `develop` PR을 만들고, 최소 한 명의 승인 후 병합합니다. 새 커밋을 올렸다면 최신 변경을 다시 검토받습니다.

## 7. 기능 완료 확인

```powershell
.\mvnw.cmd test
```

테스트 통과, migration 포함, `.env` 미포함, PR 설명 작성까지 확인합니다.
