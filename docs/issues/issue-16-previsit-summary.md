# 진료 전 요약 (#16)

개인 또는 ACTIVE 보호자가 요청한 대상의 최근 건강기록을 서버가 선정한다. 작성자가 개인인지 보호자인지와 무관하게 해당 개인의 기록을 사용한다. 클라이언트가 임의 기록 ID를 지정하는 방식은 지원하지 않는다.

## 공개 계약

- `POST /api/previsit-summaries`: JSON `{patientId?, windowDays?}`와 `Idempotency-Key`(8~100자 영문·숫자·밑줄·하이픈). 보호자는 patientId 필수, 개인은 본인 대상만 허용한다. windowDays 기본 14, 범위 1~30.
- 202와 Location, `{requestId, statusUrl: /api/ai-operations/{requestId}}`를 반환한다. 같은 작성자·키·대상·기간은 같은 요청이며 키 재사용으로 조건을 바꾸면 409다.
- `GET /api/ai-operations/{requestId}`: `{requestId,operationType:PREVISIT_SUMMARY,jobStatus,resultStatus,summaryId,retryable,errorCode}`. jobStatus는 QUEUED/RUNNING/SUCCEEDED/FAILED, resultStatus는 COMPLETE 또는 기록이 없을 때 INSUFFICIENT. summaryId는 성공 전 null이다.
- `GET /api/previsit-summaries/{summaryId}`: `{summaryId,summary,usedHealthRecordIds,windowDays,createdAt,sourceRecords:[{healthRecordId,recordedAt,content}]}`. 요청 ID와 결과 ID는 서로 다른 UUID 문자열이다.

## 기간·근거·권한

- 요청 시 서버 시각을 끝점으로 한 정확한 최근 N×24시간의 recordedAt 범위를 사용한다. 삭제된 기록·다른 개인·미래 기록은 제외한다. 최대 100건·본문 총 60,000자까지 허용하며 넘으면 일부를 몰래 버리지 않고 400 PREVISIT_003으로 기간 축소를 안내한다.
- 기록이 없으면 외부 API를 호출하지 않고 INSUFFICIENT 결과를 저장한다. 증상이나 진료 질문을 임의 생성하지 않는다.
- 서버가 선택한 원본 ID·본문·버전·기록 시각의 스냅샷을 저장한다. 요청·실행 직전·결과 저장 전·상태/상세 조회 때 현재 권한과 기록 버전을 재검증한다.
- 원본이 수정·삭제되면 이전 본문/요약을 노출하지 않고 409 PREVISIT_004로 새 요청을 안내한다. 보호자 연결 해제·탈퇴 등은 기존 건강기록 권한 규칙에 따라 거절한다.
- AI 결과의 usedHealthRecordIds가 허용 스냅샷 범위인지 검증하며 외부 ID·빈 본문·잘못된 응답은 AI_RESPONSE_INVALID로 실패시킨다. 이는 근거 ID 검증이며 모델 문장과 원문의 의미가 완전히 일치함을 보장하는 검증은 아니다.

## 실행·실패

`V202611031000__previsit_summaries.sql`은 요청/결과와 원본 스냅샷 테이블만 추가하며 기존 마이그레이션을 바꾸지 않는다. 같은 DB에서 두 Spring 인스턴스가 실행되어도 작업 행 잠금으로 선점한다. 외부 AI 호출은 트랜잭션 밖에서 수행한다.

1. AI 서버에 PREVISIT_SUMMARY_API_KEY·PREVISIT_SUMMARY_MODEL(기본 gpt-5.6-sol)을 설정한다. 문서·STT·채팅 키를 대체 사용하지 않는다.
2. Spring AI_BASE_URL과 AI_INTERNAL_TOKEN을 AI 서버와 맞춘다. 내부 요청은 `POST /internal/previsit-summaries/generate`, X-Internal-Token 인증을 사용한다.
3. Spring PREVISIT_ENABLED=true로 새 유효 기록 요약을 활성화한다. 기본 false다. 빈 기록 안내는 외부 호출 없이 가능하다.
4. PREVISIT_PROCESSING_ENABLED 기본 true로 DB 대기열을 2초 간격 처리한다. 앱을 재시작해도 QUEUED 요청은 남아 처리된다. 생성 요청의 HTTP 스레드는 AI 응답을 기다리지 않는다.
5. RUNNING 상태가 10분 지난 작업은 AI_TIMEOUT/재요청 가능으로 실패 처리한다. 자동 외부 재호출은 하지 않는다. 늦은 응답은 실행 토큰·상태가 일치해야 저장된다. 사용자 재요청은 새 키·새 요청으로 처리한다.

AI/네트워크 실패는 원문 없이 안전한 코드만 저장하며 자동 재시도하지 않는다. 공급자에 이미 접수된 요청은 로컬 시간 초과 후에도 과금될 수 있다. 호출 속도·월별 예산·스냅샷 보관 기간은 운영 배포 시 별도 설정이 필요하다.

## 프론트

홈의 고정 예시 대신 `/previsit`로 연결한다. 개인은 본인, 보호자는 ACTIVE 대상 선택 후 기간별 요약을 요청한다. 요청 키는 네트워크 재전송에 재사용한다. 계정·대상별 마지막 요청 ID만 sessionStorage에 기억하고 서버에서 권한을 다시 확인한 뒤 결과와 원본을 조회한다. 본문은 브라우저 저장소에 저장하지 않는다. 대상 변경 시 이전 요청을 중단하고 늦은 결과를 숨긴다.

프론트는 요약을 진단·처방으로 표시하지 않으며, 원본과 함께 의료진에게 확인할 참고 자료로 안내한다.

## 검증 결과 (2026-11-03)

- Spring 전체 테스트 283건 통과. #16 테스트 13건은 기간 필터, 빈 기록, 요청 키 재사용, ACTIVE 연결, 연결 해제, 원본 수정·삭제, 잘못된 AI 근거, 중단 작업 만료와 기능 비활성화를 포함한다.
- AI 전체 테스트 270건 통과·환경 의존 36건 건너뜀. 프론트 전체 18건, 타입 검사와 배포 빌드 통과.
- 별도 PostgreSQL 검증 DB에 Flyway 16개 마이그레이션을 적용했다. 실제 API 호출 1회로 최근 유효 기록 2건의 요약·근거 표시와 새로고침 복원을 확인했다. 45일 전 기록과 삭제 기록은 제외했다. 모두 가상 테스트 자료다.
- 실제 HTTP 요청에서 미인증 401·다른 개인 403, 보호자 REQUESTED 403 → ACTIVE 200 → 연결 해제 403을 확인했다. 기록이 없는 개인은 외부 호출 없이 INSUFFICIENT로 완료했다.

## 최신 develop 반영 및 재검증 (2026-11-04)

- backend develop `353e125`, AI develop `37c16c1`, frontend develop `782758e`를 각각 기능 브랜치에 병합했다. AI 환경 예시는 최신 GPT 채팅 설정과 요약 전용 키를 함께 유지했다.
- PostgreSQL 전용 검증 DB에서 신규 설치 18개 마이그레이션 적용과 최신 develop 17개 선적용 → 요약 1개 추가 적용을 확인했다. `out-of-order=false`로 마이그레이션·체크섬 검증이 통과했으며, 기존 파일이나 요약 마이그레이션 번호는 수정하지 않았다.
- Spring 전체 284건, AI 전체 285건(환경 의존 36건 건너뜀), 프론트 22건 통과. 타입 검사·프론트 배포 빌드 및 AI lint 검사 통과.
- 실제 Spring·AI 서버에서 빈 기록 INSUFFICIENT, 같은 요청 키 재사용, 전용 키 누락 FAILED/AI_API_KEY_MISSING, 원본 수정 409/PREVISIT_004, 다른 개인 403, 미인증 401을 확인했다. 이번 검증은 유료 API 호출 0회다. 실제 유료 모델 성공 검증은 위 11월 3일 결과와 구별한다.
- 프론트는 대기 중 403·404·409 등 확정 오류가 발생하면 이전 요청을 제거하고 재요청이 막히지 않도록 했다. 브라우저 저장소가 제한돼도 현재 화면의 결과 조회는 계속된다.
- AI 서버는 채팅 키가 없어도 기동하며, 채팅 호출에만 키 누락 오류를 반환한다. 요약·문서·STT 키로 채팅을 대체하지 않는다.
