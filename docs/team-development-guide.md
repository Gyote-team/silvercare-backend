# 👋 SilverCare 백엔드 팀 개발 가이드

이 문서는 **로컬 실행과 협업 절차**만 다룹니다. 코드 배치와 구현 규칙은 [개발 규약](development-convention.md)을 따릅니다.

## 시작 전

```powershell
cd backend
git switch develop
git pull origin develop
git switch -c feat/health-record
docker compose up -d
Copy-Item .env.example .env
.\run.ps1
```

- `.env`에는 카카오·JWT·DB 비밀값을 넣고 Git에 커밋하지 않습니다.
- DB는 Docker PostgreSQL 컨테이너를 사용합니다.
- `develop`, `main`에는 직접 커밋하거나 푸시하지 않습니다.

## 브랜치와 PR

```text
feat/health-record → develop → main
```

1. `feat/{domain-or-feature}` 브랜치에서 작업합니다.
2. 기능 단위로 커밋하고 `develop`을 대상으로 PR을 생성합니다.
3. 최소 한 명의 승인을 받은 뒤 병합합니다.
4. 리뷰 후 새 커밋을 올렸다면 최신 변경을 다시 검토받습니다.

## PR 전 확인

```powershell
.\mvnw.cmd test
```

- 테스트 통과
- Flyway migration 포함 여부
- `.env`·API Key·JWT Secret 미포함
- Controller에 비즈니스 로직이 없는지
- Command와 Query 책임이 섞이지 않았는지
- ErrorCode와 예외 응답 테스트가 있는지
- PR 설명에 변경 이유와 검증 결과를 적었는지
