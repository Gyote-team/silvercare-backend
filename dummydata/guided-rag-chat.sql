-- 가이드형 RAG 채팅 확인용 더미 데이터
--
-- 실행:
--   Get-Content -Raw .\dummydata\guided-rag-chat.sql |
--     docker exec -i silvercare-backend-postgres-1 psql -U silvercare -d silvercare
--
-- demo-patient(김순자), demo-caregiver(김민지) 계정과 혈액검사 2건을 만든다.
-- 고정 UUID를 사용하므로 반복 실행해도 이 파일이 만든 데이터만 갱신한다.

BEGIN;

DELETE FROM embeddings
WHERE source_id IN (
    'd1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000002',
    'd2000000-0000-0000-0000-000000000001', 'd2000000-0000-0000-0000-000000000002'
);
DELETE FROM extracted_items
WHERE analysis_id IN ('c1000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001');
DELETE FROM document_chunks
WHERE analysis_id IN ('c1000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001');
DELETE FROM document_pages
WHERE document_id IN ('b1000000-0000-0000-0000-000000000001', 'b2000000-0000-0000-0000-000000000001');
DELETE FROM document_analyses
WHERE id IN ('c1000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001');
DELETE FROM documents
WHERE id IN ('b1000000-0000-0000-0000-000000000001', 'b2000000-0000-0000-0000-000000000001');
DELETE FROM visits
WHERE id IN ('a1000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001');

INSERT INTO users (id, kakao_id, name, role, status, account_group_id)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'demo-patient', '김순자', 'PATIENT', 'ACTIVE', '10000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000002', 'demo-caregiver', '김민지', 'CAREGIVER', 'ACTIVE', '10000000-0000-0000-0000-000000000002')
ON CONFLICT (kakao_id) DO UPDATE SET
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    status = EXCLUDED.status;

-- 데모 로그인으로 이미 만들어진 계정의 실제 ID를 사용한다.
SELECT id AS demo_patient_user_id FROM users WHERE kakao_id = 'demo-patient' \gset
SELECT id AS demo_caregiver_user_id FROM users WHERE kakao_id = 'demo-caregiver' \gset

INSERT INTO patients (id, user_id, birth_date, gender, invite_code, status)
VALUES ('12000000-0000-0000-0000-000000000001', :'demo_patient_user_id', DATE '1948-03-12', 'FEMALE', 'DUMMY001PATIENT', 'ACTIVE')
ON CONFLICT (user_id) DO UPDATE SET status = EXCLUDED.status;

SELECT id AS demo_patient_id FROM patients WHERE user_id = :'demo_patient_user_id' \gset

INSERT INTO caregiver_links (id, patient_id, caregiver_user_id, relation, status, requested_at, accepted_at)
VALUES ('11000000-0000-0000-0000-000000000001', :'demo_patient_id', :'demo_caregiver_user_id', '딸', 'ACTIVE', TIMESTAMPTZ '2026-09-01 09:00:00+09', TIMESTAMPTZ '2026-09-01 09:05:00+09')
ON CONFLICT (id) DO UPDATE SET status = 'ACTIVE', accepted_at = EXCLUDED.accepted_at;

INSERT INTO visits (id, patient_id, hospital_name, department, visited_on, status, memo, created_at, updated_at)
VALUES
    ('a1000000-0000-0000-0000-000000000001', :'demo_patient_id', '은하내과', '내과', DATE '2025-10-12', 'DONE', '정기 혈액검사', TIMESTAMPTZ '2025-10-12 09:00:00+09', TIMESTAMPTZ '2025-10-12 09:00:00+09'),
    ('a2000000-0000-0000-0000-000000000001', :'demo_patient_id', '은하내과', '내과', DATE '2026-09-18', 'DONE', '정기 혈액검사 및 당 조절 확인', TIMESTAMPTZ '2026-09-18 09:00:00+09', TIMESTAMPTZ '2026-09-18 09:00:00+09')
ON CONFLICT (id) DO UPDATE SET visited_on = EXCLUDED.visited_on, memo = EXCLUDED.memo, status = EXCLUDED.status;

INSERT INTO documents (id, visit_id, patient_id, uploader_user_id, storage_key, file_name, mime_type, file_size_bytes, document_type, status, status_changed_at, created_at, updated_at)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', :'demo_patient_id', :'demo_patient_user_id', 'dummy/rag/lab-2025-10.pdf', '2025-10-12_혈액검사결과.pdf', 'application/pdf', 120000, 'LAB_RESULT', 'READY', TIMESTAMPTZ '2025-10-12 10:00:00+09', TIMESTAMPTZ '2025-10-12 10:00:00+09', TIMESTAMPTZ '2025-10-12 10:00:00+09'),
    ('b2000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001', :'demo_patient_id', :'demo_patient_user_id', 'dummy/rag/lab-2026-09.pdf', '2026-09-18_혈액검사결과.pdf', 'application/pdf', 124000, 'LAB_RESULT', 'READY', TIMESTAMPTZ '2026-09-18 10:00:00+09', TIMESTAMPTZ '2026-09-18 10:00:00+09', TIMESTAMPTZ '2026-09-18 10:00:00+09')
ON CONFLICT (id) DO UPDATE SET file_name = EXCLUDED.file_name, status = 'READY', deleted_at = NULL, patient_id = EXCLUDED.patient_id;

INSERT INTO document_pages (id, document_id, page_no, ocr_status, ocr_text, width_px, height_px, created_at)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 1, 'SUCCEEDED', E'혈액검사 결과 (2025-10-12)\n헤모글로빈(Hb) 11.2 g/dL  [참고치 12.0~16.0]\n당화혈색소(HbA1c) 7.1 %  [참고치 4.0~5.6]', 1240, 1754, TIMESTAMPTZ '2025-10-12 10:01:00+09'),
    ('e2000000-0000-0000-0000-000000000001', 'b2000000-0000-0000-0000-000000000001', 1, 'SUCCEEDED', E'혈액검사 결과 (2026-09-18)\n헤모글로빈(Hb) 12.6 g/dL  [참고치 12.0~16.0]\n당화혈색소(HbA1c) 6.5 %  [참고치 4.0~5.6]', 1240, 1754, TIMESTAMPTZ '2026-09-18 10:01:00+09');

INSERT INTO document_analyses (id, document_id, parser_version, model_version, status, confidence, started_at, completed_at, created_at)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'dummy-chunker-1.0', 'dummy-embedding-1.0', 'SUCCEEDED', 0.9900, TIMESTAMPTZ '2025-10-12 10:02:00+09', TIMESTAMPTZ '2025-10-12 10:02:05+09', TIMESTAMPTZ '2025-10-12 10:02:00+09'),
    ('c2000000-0000-0000-0000-000000000001', 'b2000000-0000-0000-0000-000000000001', 'dummy-chunker-1.0', 'dummy-embedding-1.0', 'SUCCEEDED', 0.9900, TIMESTAMPTZ '2026-09-18 10:02:00+09', TIMESTAMPTZ '2026-09-18 10:02:05+09', TIMESTAMPTZ '2026-09-18 10:02:00+09');

INSERT INTO document_chunks (id, analysis_id, page_id, chunk_index, content, bounding_box, token_count, created_at)
VALUES
    ('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 0, '2025년 10월 혈액검사에서 헤모글로빈은 11.2 g/dL이며 참고치는 12.0~16.0 g/dL입니다.', '{"x":100,"y":220,"width":840,"height":90}'::jsonb, 30, TIMESTAMPTZ '2025-10-12 10:02:03+09'),
    ('d1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 1, '2025년 10월 당화혈색소는 7.1 %이며 참고치는 4.0~5.6 %입니다.', '{"x":100,"y":330,"width":840,"height":90}'::jsonb, 27, TIMESTAMPTZ '2025-10-12 10:02:03+09'),
    ('d2000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000001', 0, '2026년 9월 혈액검사에서 헤모글로빈은 12.6 g/dL이며 참고치는 12.0~16.0 g/dL입니다.', '{"x":100,"y":220,"width":840,"height":90}'::jsonb, 30, TIMESTAMPTZ '2026-09-18 10:02:03+09'),
    ('d2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000001', 1, '2026년 9월 당화혈색소는 6.5 %이며 참고치는 4.0~5.6 %입니다.', '{"x":100,"y":330,"width":840,"height":90}'::jsonb, 27, TIMESTAMPTZ '2026-09-18 10:02:03+09');

INSERT INTO extracted_items (id, analysis_id, page_id, item_type, normalized_name, raw_value, normalized_value, unit, reference_range, confidence, source_text, source_box, created_at)
VALUES
    ('f1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'LAB_VALUE', '헤모글로빈', '11.2', '11.2', 'g/dL', '12.0~16.0', 0.9900, '헤모글로빈(Hb) 11.2 g/dL  [참고치 12.0~16.0]', '{"x":100,"y":220,"width":840,"height":90}'::jsonb, TIMESTAMPTZ '2025-10-12 10:02:04+09'),
    ('f1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'LAB_VALUE', '당화혈색소', '7.1', '7.1', '%', '4.0~5.6', 0.9900, '당화혈색소(HbA1c) 7.1 %  [참고치 4.0~5.6]', '{"x":100,"y":330,"width":840,"height":90}'::jsonb, TIMESTAMPTZ '2025-10-12 10:02:04+09'),
    ('f2000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000001', 'LAB_VALUE', '헤모글로빈', '12.6', '12.6', 'g/dL', '12.0~16.0', 0.9900, '헤모글로빈(Hb) 12.6 g/dL  [참고치 12.0~16.0]', '{"x":100,"y":220,"width":840,"height":90}'::jsonb, TIMESTAMPTZ '2026-09-18 10:02:04+09'),
    ('f2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000001', 'LAB_VALUE', '당화혈색소', '6.5', '6.5', '%', '4.0~5.6', 0.9900, '당화혈색소(HbA1c) 6.5 %  [참고치 4.0~5.6]', '{"x":100,"y":330,"width":840,"height":90}'::jsonb, TIMESTAMPTZ '2026-09-18 10:02:04+09');

-- 실제 벡터 검색은 아직 연결 전이지만, 청킹·임베딩 완료 상태를 재현하는 768차원 벡터 행이다.
INSERT INTO embeddings (id, source_type, source_id, patient_id, visit_id, embedding, model_name, dimensions, created_at)
VALUES
    ('91000000-0000-0000-0000-000000000001', 'DOCUMENT_CHUNK', 'd1000000-0000-0000-0000-000000000001', :'demo_patient_id', 'a1000000-0000-0000-0000-000000000001', array_fill(0::real, ARRAY[768])::vector, 'dummy-embedding-768', 768, TIMESTAMPTZ '2025-10-12 10:02:05+09'),
    ('91000000-0000-0000-0000-000000000002', 'DOCUMENT_CHUNK', 'd1000000-0000-0000-0000-000000000002', :'demo_patient_id', 'a1000000-0000-0000-0000-000000000001', array_fill(0::real, ARRAY[768])::vector, 'dummy-embedding-768', 768, TIMESTAMPTZ '2025-10-12 10:02:05+09'),
    ('92000000-0000-0000-0000-000000000001', 'DOCUMENT_CHUNK', 'd2000000-0000-0000-0000-000000000001', :'demo_patient_id', 'a2000000-0000-0000-0000-000000000001', array_fill(0::real, ARRAY[768])::vector, 'dummy-embedding-768', 768, TIMESTAMPTZ '2026-09-18 10:02:05+09'),
    ('92000000-0000-0000-0000-000000000002', 'DOCUMENT_CHUNK', 'd2000000-0000-0000-0000-000000000002', :'demo_patient_id', 'a2000000-0000-0000-0000-000000000001', array_fill(0::real, ARRAY[768])::vector, 'dummy-embedding-768', 768, TIMESTAMPTZ '2026-09-18 10:02:05+09');

-- AI 서버를 호출하지 않아도 채팅과 원문 근거 화면을 확인할 수 있는 예시 답변이다.
INSERT INTO ai_result_documents (
    id, request_id, patient_id, requested_by_user_id, question, question_type,
    job_status, result_status, title, content, citation_json, created_at, completed_at
)
VALUES
    ('ad000000-0000-0000-0000-000000000001', 'ae000000-0000-0000-0000-000000000001', :'demo_patient_id', :'demo_patient_user_id',
     '최근 혈액검사 결과를 쉽게 설명해 주세요', 'DOCUMENT_EASY_EXPLANATION', 'SUCCEEDED', 'COMPLETE',
     '최근 혈액검사 설명', '2026년 9월 검사에서 당화혈색소(HbA1c)는 6.5%로 기록되어 있습니다.',
     '{"sentences":[{"text":"2026년 9월 검사에서 당화혈색소(HbA1c)는 6.5%로 기록되어 있습니다.","citation_ids":["ab000000-0000-0000-0000-000000000001"]}],"sources":[{"citation_id":"ab000000-0000-0000-0000-000000000001","document_id":"b2000000-0000-0000-0000-000000000001","page_no":1,"label":"당화혈색소","source_text":"당화혈색소(HbA1c) 6.5 %","value":"6.5","unit":"%"}]}',
     TIMESTAMPTZ '2026-09-18 10:10:00+09', TIMESTAMPTZ '2026-09-18 10:10:05+09'),
    ('ad000000-0000-0000-0000-000000000002', 'ae000000-0000-0000-0000-000000000002', :'demo_patient_id', :'demo_caregiver_user_id',
     '지난번 검사와 이번 검사를 비교해 주세요', 'LAB_RESULT_COMPARISON', 'SUCCEEDED', 'COMPLETE',
     '혈액검사 비교', '당화혈색소는 2025년 10월 7.1%에서 2026년 9월 6.5%로 기록되어 0.6%p 낮아졌습니다.',
     '{"sentences":[{"text":"당화혈색소는 2025년 10월 7.1%에서 2026년 9월 6.5%로 기록되어 0.6%p 낮아졌습니다.","citation_ids":["ab000000-0000-0000-0000-000000000002","ab000000-0000-0000-0000-000000000003"]}],"sources":[{"citation_id":"ab000000-0000-0000-0000-000000000002","document_id":"b1000000-0000-0000-0000-000000000001","page_no":1,"label":"이전 당화혈색소","source_text":"당화혈색소(HbA1c) 7.1 %","value":"7.1","unit":"%"},{"citation_id":"ab000000-0000-0000-0000-000000000003","document_id":"b2000000-0000-0000-0000-000000000001","page_no":1,"label":"최근 당화혈색소","source_text":"당화혈색소(HbA1c) 6.5 %","value":"6.5","unit":"%"}]}',
     TIMESTAMPTZ '2026-09-18 10:20:00+09', TIMESTAMPTZ '2026-09-18 10:20:05+09')
ON CONFLICT (id) DO UPDATE SET
    patient_id = EXCLUDED.patient_id,
    requested_by_user_id = EXCLUDED.requested_by_user_id,
    content = EXCLUDED.content,
    citation_json = EXCLUDED.citation_json;

INSERT INTO chat_sessions (id, patient_id, started_by_user_id, title, created_at)
VALUES
    ('ac000000-0000-0000-0000-000000000001', :'demo_patient_id', :'demo_patient_user_id', '최근 혈액검사 설명', TIMESTAMPTZ '2026-09-18 10:09:00+09'),
    ('ac000000-0000-0000-0000-000000000002', :'demo_patient_id', :'demo_caregiver_user_id', '지난 검사와 비교', TIMESTAMPTZ '2026-09-18 10:19:00+09')
ON CONFLICT (id) DO UPDATE SET patient_id = EXCLUDED.patient_id, started_by_user_id = EXCLUDED.started_by_user_id;

INSERT INTO chat_messages (id, session_id, role, content, result_document_id, created_at)
VALUES
    ('af000000-0000-0000-0000-000000000001', 'ac000000-0000-0000-0000-000000000001', 'USER', '최근 혈액검사 결과를 쉽게 설명해 주세요', NULL, TIMESTAMPTZ '2026-09-18 10:09:01+09'),
    ('af000000-0000-0000-0000-000000000002', 'ac000000-0000-0000-0000-000000000001', 'ASSISTANT', '2026년 9월 검사에서 당화혈색소(HbA1c)는 6.5%로 기록되어 있습니다.', 'ad000000-0000-0000-0000-000000000001', TIMESTAMPTZ '2026-09-18 10:10:05+09'),
    ('af000000-0000-0000-0000-000000000003', 'ac000000-0000-0000-0000-000000000002', 'USER', '지난번 검사와 이번 검사를 비교해 주세요', NULL, TIMESTAMPTZ '2026-09-18 10:19:01+09'),
    ('af000000-0000-0000-0000-000000000004', 'ac000000-0000-0000-0000-000000000002', 'ASSISTANT', '당화혈색소는 2025년 10월 7.1%에서 2026년 9월 6.5%로 기록되어 0.6%p 낮아졌습니다.', 'ad000000-0000-0000-0000-000000000002', TIMESTAMPTZ '2026-09-18 10:20:05+09')
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, result_document_id = EXCLUDED.result_document_id;

COMMIT;
