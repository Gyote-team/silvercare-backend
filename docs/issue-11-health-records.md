# 건강기록 API와 계정 전환

작업 브랜치: 백엔드·프론트엔드 모두 `feat/account-switch`.

## 계정과 데이터

기존 account-switch 구조를 유지한다. 한 로그인에 개인·보호자 역할별 사용자 행을 연결하고,
개인 역할의 건강 프로필에 기록을 저장한다. 계정 전환 시 세션과 JWT를 함께 바꾼다.
개인 화면에서는 보호자 연결 없이 본인 기록을 관리하며, 보호자 화면에서는 ACTIVE로 연결된 개인을 선택한다.
화면에서 작성자를 임의 선택할 수 없으며 작성자는 현재 인증된 계정으로 결정한다.
관계가 해제되어도 개인의 기록은 유지된다.

## HTTP 계약

| 요청 | 응답 |
|---|---|
| POST /api/health-records | 201, 기록 |
| GET /api/health-records?patientId=&cursor=&size= | 200, items·nextCursor·hasNext |
| GET /api/health-records/{recordId} | 200, 기록 |
| PUT /api/health-records/{recordId} | 200, 수정된 기록 |
| DELETE /api/health-records/{recordId} | 204 |

작성: `{ "patientId": "UUID (보호자 필수)", "body": "1~2000자", "visitId": "UUID 또는 null" }`.
수정: body와 visitId를 변경하며 대상 개인·작성자는 변경하지 않는다.
본문은 공백만 입력할 수 없고, 개인 계정의 patientId 생략은 본인 프로필을 뜻한다.
기록 필드: recordId, patientId, authorUserId, authorName, visitId, body,
recordedAt, createdAt, updatedAt, proxyWritten.
UUID는 JSON 문자열, 시각은 ISO-8601이다.

목록은 createdAt DESC, id DESC 순서이며 size 기본 20·최대 100이다.
nextCursor는 대상 개인·시각·UUID를 포함하는 불투명 문자열이다.
다른 개인의 커서는 거절한다. 같은 시각의 여러 기록도 누락 없이 이어서 조회한다.
삭제된 기록은 목록·상세에서 제외하고 DB 행과 참조는 보존한다.
향후 타임라인·AI 입력 조회에서도 health_records.deleted_at IS NULL을 반드시 적용해야 한다.

## 권한과 오류

- 개인은 본인 프로필의 기록만 작성·조회한다.
- 보호자는 요청 대상과 ACTIVE 관계일 때만 작성·조회한다.
- 개인은 본인 프로필의 모든 기록을 작성자와 무관하게 수정·삭제할 수 있다. 보호자 연결 없이도 가능하다.
- 보호자는 현재 ACTIVE로 접근 가능하고 본인이 작성한 기록만 수정·삭제할 수 있다.
- 선택 방문은 대상 개인의 방문이어야 하며 삭제·취소된 방문은 거절한다.
- 400: 잘못된 본문·size·cursor·UUID·방문.
- 401: 인증 없음.
- 403: 대상 접근 권한 없음 또는 작성자가 아님.
- 404: 없거나 삭제된 기록.
- 409: 겹친 수정·삭제로 발생한 낙관적 잠금 충돌.

## DB 변경

`V202610081200__health_record_crud.sql`: health_records.version 추가,
삭제되지 않은 기록의 대상별 최신순 조회 인덱스 추가. 기존 migration은 변경하지 않는다.

## 확인 방법

1. 개인 계정으로 보호자 연결 없이 기록 작성·수정·삭제를 확인한다.
2. 계정 메뉴에서 보호자로 전환하고, 연결된 개인이 없으면 본인 기록이 노출되지 않는지 확인한다.
3. 개인으로 돌아와 기존 기록이 유지되는지 확인한다.
4. 보호자 계정에 서로 다른 개인 2명을 연결하고 선택 대상을 바꿔 기록이 섞이지 않는지 확인한다.
5. 개인은 보호자 작성 기록도 수정·삭제할 수 있고, 보호자는 다른 작성자의 기록을 변경할 수 없는지 확인한다.
6. 연결 해제 후 보호자의 이전 기록 접근이 차단되고 개인에게 기록은 유지되는지 확인한다.

자동 검증: HealthRecordServiceTest(권한·분리·커서·삭제),
HealthRecordApiIntegrationTest(실제 인증 필터·CRUD·세션/JWT 계정 전환).
프론트 화면은 /records에서 실제 API를 사용한다.

2026-10-08 검증: 백엔드 전체 85개 테스트 통과, 프론트 TypeScript 검사 및 production build 통과.
테스트 DB는 H2이며 PostgreSQL의 실제 Flyway 적용은 Docker가 실행되지 않아 확인하지 못했다.

2026-10-10: 최신 develop(백엔드 b637da3, 프론트 5d576bd)을 각 account-switch 브랜치에 병합했다.
문서 업로드·분석과 프론트 FormData 지원을 보존했다.
수정 요청의 patientId가 원래 대상과 다르면 400을 반환하고 기록을 변경하지 않는다.
프론트 조회 실패는 빈 기록 상태와 구분하고 다시 불러오기를 제공한다.
대상 개인 전환 시 작성 중인 내용이 있으면 삭제 여부를 확인한다.
백엔드 전체 199개 테스트 통과.

## PR 전 DB 업그레이드 확인 (2026-10-10)

- Docker Compose 설정은 변경하지 않는다. PostgreSQL 이미지는 `pgvector/pgvector:pg16`이다.
- V1/V2/V3를 포함한 develop의 기존 SQL은 수정하지 않는다.
- 계정 전환의 `V202610052010`은 develop의 `V202610052216`보다 낮다. 이미 develop을 적용한 DB에서도 누락 없이 실행하도록 Flyway `out-of-order: true`를 사용한다. 기존 사용자 행을 보존하고 account_group_id를 채우는 추가 마이그레이션이다.
- 기존 로컬 `silvercare` DB는 오래된 V1 체크섬(-1425741100)을 갖고 있어 현재 V1(1815981943)과 다르다. 이 환경은 자동 repair나 볼륨 삭제로 해결하지 않는다. 데이터가 필요한 경우 별도 백업 및 변환이 필요하다.
- 현재 시연 서버는 `DB_URL=jdbc:postgresql://127.0.0.1:5432/silvercare_issue11`을 사용한다. 이 DB에는 V3와 추가 마이그레이션이 모두 적용돼 있다. 이 설정은 로컬 실행 환경이며 저장소의 기본 DB 주소를 바꾸지 않는다.
- 시스템 알림은 `V202610101630__system_notifications.sql`의 별도 테이블을 사용한다. 기존 복약 발송용 notifications 테이블과 충돌하지 않는다.
- 개인이 보호자 작성 기록을 수정해도 원래 authorUserId와 authorName을 유지하는 HTTP 회귀 테스트를 추가했다.
- 백엔드 전체 203개 테스트와 프론트 TypeScript 검사를 통과했다.
- 임시 PostgreSQL DB에 develop의 6개 마이그레이션과 기존 계정 한 개를 적용한 뒤 현재 브랜치로 업그레이드했다. 추가 3개 마이그레이션이 성공했고 기존 계정 및 그룹 ID가 유지됐으며 JPA 스키마 검증과 서버 기동을 통과했다.
- 프론트 production build도 통과했다.

## 시스템 알림 계약

`GET /api/notifications`는 현재 역할 계정의 최신 50개 알림과 전체 unreadCount를 반환한다.
`PATCH /api/notifications/{id}/read`와 `PATCH /api/notifications/read-all`은 204를 반환한다.
타인의 알림 읽음 요청은 404이며, 계정 전환 시 다른 역할 계정의 알림을 노출하지 않는다.
연결 요청·수락·거절·취소·해제와 보호자 기록 작성이 대상자에게 알림을 생성한다.
알림은 원래 활동과 같은 트랜잭션에서 저장되며 건강기록 본문을 복제하지 않는다.
기존 활동은 소급 생성하지 않는다.
