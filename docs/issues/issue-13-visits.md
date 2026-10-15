# 병원 방문 API (FC-06)

개인은 보호자 없이 본인 방문을 관리한다. 보호자는 대상 개인과 ACTIVE로 연결된 동안 작성·조회·수정·취소할 수 있다. 연결 해제 후 보호자의 접근은 차단되며 개인의 방문·기록·문서는 유지된다.

## HTTP 계약

- POST `/api/visits`: 201, 방문 상세
- GET `/api/visits?patientId=&cursor=&size=&includeCanceled=&visitStatus=`: 200, items·nextCursor·hasNext
- GET `/api/visits/{visitId}`: 200, 방문 상세 (취소 방문도 조회 가능)
- PUT `/api/visits/{visitId}`: 200, 수정된 방문 상세
- DELETE `/api/visits/{visitId}`: 204, CANCELED 전환 (반복 요청도 204)

```json
{
  "patientId": "보호자 필수, 개인 생략 가능 UUID",
  "visitedOn": "2026-10-13",
  "hospitalName": "병원명",
  "department": "내과",
  "memo": "선택 메모",
  "visitStatus": "DONE",
  "version": 0
}
```

visitedOn과 visitStatus는 필수다. hospitalName·department·memo는 선택이며 최대 길이는 200·100·2000자다. 날짜는 Asia/Seoul의 오늘을 기준으로 판단한다. 미래 날짜는 SCHEDULED만 등록·수정 가능하다. DELETE를 통한 미래 예약 취소는 가능하다. 취소된 방문은 수정·복구하지 않으며 필요한 경우 새 방문을 등록한다. 대상 개인 변경은 거절한다.

등록·수정에는 SCHEDULED와 DONE만 허용한다. CANCELED는 날짜와 관계없이 400 GLOBAL_002로 거절하며, 취소는 DELETE로만 수행한다. 보호자는 등록과 수정 모두 patientId가 필수다.

응답에는 visitId·patientId·visitedOn·hospitalName·department·memo·visitStatus·documentIds·healthRecordIds·createdAt·updatedAt·version을 제공한다. 모든 ID는 UUID 문자열이다. 삭제된 문서 및 건강기록은 참조 목록에서 제외한다. 취소는 방문의 상태만 바꾸며 하위 자료를 삭제하거나 FK를 끊지 않는다.

version은 등록 시 생략할 수 있지만 수정 시 조회 응답의 값을 반드시 보낸다. 누락·음수는 400 GLOBAL_002, 현재 저장된 버전과 다르면 409 GLOBAL_004로 거절한다. 수정 응답은 증가한 version을 반환한다. 이는 오래된 화면의 순차 저장과 트랜잭션이 겹친 동시 수정을 모두 방어한다.

목록은 방문일 DESC, id DESC 순서다. size 기본 20·최대 100, includeCanceled 기본 false다. 커서는 대상 개인과 취소 필터에 묶이며 다른 개인이나 필터에 재사용하면 400이다. 동일한 방문일도 UUID로 구분해 누락 없이 조회한다.

visitStatus는 선택이며 SCHEDULED·DONE·CANCELED 중 한 상태만 조회할 수 있다. 필터는 페이지네이션 이전에 적용하고 커서에도 묶인다. 취소된 방문만 보려면 visitStatus=CANCELED와 includeCanceled=true를 함께 전달한다. 프론트는 기본 예정·완료, 전체, 방문 예정, 방문 완료, 취소된 방문을 선택하도록 제공한다.

## 상태 및 오류

- SCHEDULED: 예약
- DONE: 방문 완료
- CANCELED: 취소
- 400 GLOBAL_002: 입력 형식·길이·대상 변경·size·cursor 오류
- 400 VISIT_003: 미래 날짜와 상태 조합 오류
- 401: 인증 없음
- 403 VISIT_002: 대상 접근 불가
- 404 VISIT_001: 없거나 soft delete된 방문
- 409 VISIT_004: 취소 방문 수정
- 409 GLOBAL_004: 동시 수정·취소의 버전 충돌

## DB 적용

V202610141200__visit_crud_status_and_version.sql을 추가한다. 기존 SQL은 변경하지 않는다. PLANNED→SCHEDULED, COMPLETED→DONE, CANCELLED→CANCELED로 변환하고 OPEN은 미래 날짜면 SCHEDULED, 그 외 DONE으로 바꾼다. 기존 행·하위 FK를 보존하며 버전과 방문일 기준 목록 인덱스를 추가한다.

미병합 방문 마이그레이션의 번호는 develop의 V202610132247 뒤로 조정했다. out-of-order 설정은 사용하지 않는다. 옛 번호 V202610131400을 적용했던 로컬 검증 DB는 이력이 다르므로 그대로 재사용하지 말고 별도로 확인한다. 팀 공통 DB의 이력을 자동 수정하는 SQL은 포함하지 않는다.

상태 변환 후 건강기록의 방문 유효성 검사도 CANCELED를 기준으로 한다. 취소 방문에 새 건강기록을 연결하는 요청은 기존 건강기록 규칙에 따라 거절한다. 기존 연결 기록 조회는 유지된다.

## 확인 시나리오

1. 보호자 없는 개인의 방문 작성·조회·수정·취소
2. 미래 방문 상태 제한 및 반복 취소
3. REQUESTED/ACTIVE/REVOKED 보호자의 권한 및 대상 변경 거절
4. 같은 날짜의 커서 목록, 취소 포함 필터, 다른 대상 커서 차단
5. 연결된 문서·건강기록을 가진 방문 취소 후 참조 보존
6. 삭제된 하위 자료 필터와 동시 변경 충돌

자동 검증은 VisitApiIntegrationTest에서 실제 인증 필터와 공통 HTTP 오류 계약을 포함한다. 프론트는 /timeline의 방문 관리와 /visits/{visitId}의 상세·수정·취소 화면에서 연동하며, 수정할 때 조회한 version을 전송한다.
