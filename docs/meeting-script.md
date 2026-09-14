# 🗣️ 백엔드 구조 설명 대본

## 1분 설명

이번 백엔드는 기능별 도메인 구조를 먼저 나누고, 각 도메인 안에서 Command와 Query를 분리하는 점진적 CQRS 방식으로 구성했습니다.

예를 들어 건강기록 기능을 구현하면 `health_record` 폴더 안에 Controller와 DTO를 위한 `api`, 상태를 변경하는 `command`, 읽기 전용 조회를 담당하는 `query`, Entity와 Repository가 있는 `domain`, 오류 코드를 두는 `error`를 같은 기준으로 추가합니다. 그래서 팀원이 새 기능을 맡아도 파일을 어디에 둬야 할지 바로 알 수 있습니다.

Command는 생성·수정·삭제처럼 데이터를 바꾸는 로직만 담당하고, Query는 목록·상세 조회처럼 데이터를 읽는 로직만 담당합니다. 다만 지금 단계에서는 복잡도를 높이지 않기 위해 DB는 하나의 PostgreSQL을 공유합니다. 읽기 DB 분리나 이벤트 소싱은 현재 도입하지 않았습니다.

Controller는 HTTP 요청과 응답만 처리하고, Query 결과는 `query/model`로 만든 뒤 `api/mapper`에서 HTTP Response DTO로 변환합니다. 이렇게 해서 비즈니스·조회 계층이 웹 API 형식에 직접 의존하지 않도록 했습니다.

DB 구조 변경은 JPA 자동 생성이 아니라 Flyway SQL migration으로 관리합니다. 협업·배포 시 같은 DB 변경 이력을 순서대로 적용할 수 있기 때문입니다. 실제 데이터 조회와 저장은 JPA Entity와 Repository로 처리합니다.

예외는 도메인별 ErrorCode와 공통 BusinessException으로 통일했습니다. 그래서 어떤 기능이든 같은 JSON 오류 형식과 HTTP 상태 코드로 응답할 수 있습니다.

마지막으로 팀 개발 가이드와 패키지 규약 문서를 추가했고, `develop`과 `main`은 PR과 최소 한 명 승인 후에만 병합되도록 설정했습니다.

## 한 문장 요약

**도메인별 공통 구조와 Command/Query 분리로 팀원의 파일 배치 기준을 통일하고, Flyway·공통 예외 처리·PR 규칙으로 협업 안정성을 확보했습니다.**

## 예상 질문과 짧은 답변

### 왜 완전한 CQRS가 아닌가요?

현재는 코드 책임만 Command와 Query로 나누는 점진적 CQRS입니다. 기능과 트래픽이 아직 크지 않아 읽기 DB·이벤트 소싱·메시지 브로커까지 도입하면 복잡도만 커지기 때문입니다.

### 왜 테이블을 JPA 자동 생성으로 만들지 않나요?

JPA는 Entity 매핑과 CRUD에 사용하고, 테이블 구조 변경은 Flyway SQL로 버전 관리합니다. 그래야 팀원·개발·운영 환경이 동일한 DB 변경 이력을 적용할 수 있습니다.

### Query Model과 Response DTO를 분리한 이유는 뭔가요?

조회 로직이 HTTP 응답 형식에 묶이지 않도록 하기 위해서입니다. 화면이나 API 형식이 바뀌어도 Query Service의 핵심 조회 로직은 그대로 재사용할 수 있습니다.

### 예외 처리는 어떻게 통일했나요?

도메인별 ErrorCode에 HTTP 상태·오류 코드·메시지를 정의하고, Service에서 BusinessException을 발생시키면 GlobalExceptionHandler가 공통 JSON 응답으로 변환합니다.
