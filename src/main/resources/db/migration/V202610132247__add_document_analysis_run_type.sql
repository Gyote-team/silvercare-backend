-- AI 서버의 문서 분석 VLM 호출 기록용으로 run_type에 DOCUMENT_ANALYSIS를 추가한다(노션 FC-15 operationType 기준).
ALTER TABLE ai_runs
    DROP CONSTRAINT ai_runs_type_check;

ALTER TABLE ai_runs
    ADD CONSTRAINT ai_runs_type_check
        CHECK (run_type IN ('EXPLANATION', 'CHAT', 'COMPARISON', 'VALIDATION', 'PRE_VISIT_SUMMARY', 'DOCUMENT_ANALYSIS'));
