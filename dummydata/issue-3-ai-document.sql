-- 이슈 3 AI 의료문서 조회 화면 확인용 더미 데이터
--
-- 실행:
--   docker exec -i silvercare-postgres-1 psql -U silvercare -d silvercare < dummydata/issue-3-ai-document.sql
--
-- demo-patient / demo-caregiver 사용자가 없으면 로컬 확인용 계정을 함께 만든다.
-- 고정 UUID를 사용하므로 같은 파일을 여러 번 실행해도 동일한 데이터만 갱신한다.

BEGIN;

DO $$
DECLARE
    patient_user_id UUID;
    caregiver_user_id UUID;
    patient_id UUID;
    caregiver_link_id UUID := '11000000-0000-0000-0000-000000000001';
    visit_id UUID := '22000000-0000-0000-0000-000000000001';
    document_id UUID := '33000000-0000-0000-0000-000000000001';
    analysis_id UUID := '44000000-0000-0000-0000-000000000001';
    page_id UUID := '55000000-0000-0000-0000-000000000001';
    lab_item_id UUID := '66000000-0000-0000-0000-000000000001';
    medication_item_id UUID := '66000000-0000-0000-0000-000000000002';
    ai_run_id UUID := '77000000-0000-0000-0000-000000000001';
    explanation_id UUID := '88000000-0000-0000-0000-000000000001';
    lab_section_id UUID := '99000000-0000-0000-0000-000000000001';
    medication_section_id UUID := '99000000-0000-0000-0000-000000000002';
    lab_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000001';
    medication_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000002';
    lab_citation_id UUID := 'bb000000-0000-0000-0000-000000000001';
    medication_citation_id UUID := 'bb000000-0000-0000-0000-000000000002';
BEGIN
    -- 데모 환자·보호자 계정이 없을 때만 로컬 확인용 계정을 생성한다.
    SELECT id INTO patient_user_id FROM users WHERE kakao_id = 'demo-patient';
    IF patient_user_id IS NULL THEN
        patient_user_id := '10000000-0000-0000-0000-000000000001';
        INSERT INTO users (id, kakao_id, name, role, status)
        VALUES (patient_user_id, 'demo-patient', '김순자', 'PATIENT', 'ACTIVE')
        ON CONFLICT (kakao_id) DO UPDATE SET name = EXCLUDED.name, role = EXCLUDED.role, status = EXCLUDED.status;
    END IF;

    SELECT id INTO caregiver_user_id FROM users WHERE kakao_id = 'demo-caregiver';
    IF caregiver_user_id IS NULL THEN
        caregiver_user_id := '10000000-0000-0000-0000-000000000002';
        INSERT INTO users (id, kakao_id, name, role, status)
        VALUES (caregiver_user_id, 'demo-caregiver', '김민지', 'CAREGIVER', 'ACTIVE')
        ON CONFLICT (kakao_id) DO UPDATE SET name = EXCLUDED.name, role = EXCLUDED.role, status = EXCLUDED.status;
    END IF;

    SELECT id INTO patient_id FROM patients WHERE user_id = patient_user_id;
    IF patient_id IS NULL THEN
        patient_id := '12000000-0000-0000-0000-000000000001';
        INSERT INTO patients (id, user_id, birth_date, gender, invite_code, status)
        VALUES (patient_id, patient_user_id, DATE '1948-03-12', 'FEMALE', 'DUMMY001PATIENT', 'ACTIVE')
        ON CONFLICT (user_id) DO NOTHING;
        SELECT id INTO patient_id FROM patients WHERE user_id = patient_user_id;
    END IF;

    -- 보호자가 동일 문서를 조회할 수 있도록 활성 돌봄 관계를 만든다.
    INSERT INTO caregiver_links (id, patient_id, caregiver_user_id, relation, status, requested_at, accepted_at)
    VALUES (
        caregiver_link_id,
        patient_id,
        caregiver_user_id,
        '딸',
        'ACTIVE',
        TIMESTAMPTZ '2026-09-27 09:00:00+09',
        TIMESTAMPTZ '2026-09-27 09:05:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        patient_id = EXCLUDED.patient_id,
        caregiver_user_id = EXCLUDED.caregiver_user_id,
        relation = EXCLUDED.relation,
        status = EXCLUDED.status,
        accepted_at = EXCLUDED.accepted_at;

    INSERT INTO visits (id, patient_id, hospital_name, department, visited_on, status, memo, created_at, updated_at)
    VALUES (
        visit_id,
        patient_id,
        '서울 실버내과',
        '내과',
        DATE '2026-09-28',
        'COMPLETED',
        '정기 혈액검사 결과 확인',
        TIMESTAMPTZ '2026-09-28 10:00:00+09',
        TIMESTAMPTZ '2026-09-28 10:00:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        patient_id = EXCLUDED.patient_id,
        hospital_name = EXCLUDED.hospital_name,
        department = EXCLUDED.department,
        visited_on = EXCLUDED.visited_on,
        status = EXCLUDED.status,
        memo = EXCLUDED.memo;

    INSERT INTO documents (
        id, visit_id, uploader_user_id, storage_key, file_name, mime_type,
        file_size_bytes, document_type, status, created_at, updated_at
    )
    VALUES (
        document_id,
        visit_id,
        patient_user_id,
        'dummy/issue-3/lab-result-2026-09-28.pdf',
        '2026-09-28_혈액검사결과지.pdf',
        'application/pdf',
        184320,
        'LAB_RESULT',
        'READY',
        TIMESTAMPTZ '2026-09-28 11:00:00+09',
        TIMESTAMPTZ '2026-09-28 11:00:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        visit_id = EXCLUDED.visit_id,
        uploader_user_id = EXCLUDED.uploader_user_id,
        file_name = EXCLUDED.file_name,
        document_type = EXCLUDED.document_type,
        status = EXCLUDED.status,
        deleted_at = NULL;

    INSERT INTO document_pages (
        id, document_id, page_no, ocr_status, ocr_text, width_px, height_px, created_at
    )
    VALUES (
        page_id,
        document_id,
        1,
        'SUCCEEDED',
        '총 콜레스테롤 205 mg/dL, LDL 콜레스테롤 128 mg/dL, 공복혈당 108 mg/dL',
        1654,
        2339,
        TIMESTAMPTZ '2026-09-28 11:01:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        ocr_status = EXCLUDED.ocr_status,
        ocr_text = EXCLUDED.ocr_text;

    INSERT INTO document_analyses (
        id, document_id, parser_version, model_version, status, confidence,
        started_at, completed_at, created_at
    )
    VALUES (
        analysis_id,
        document_id,
        'dummy-parser-1.0',
        'dummy-llm-1.0',
        'SUCCEEDED',
        0.9700,
        TIMESTAMPTZ '2026-09-28 11:02:00+09',
        TIMESTAMPTZ '2026-09-28 11:02:08+09',
        TIMESTAMPTZ '2026-09-28 11:02:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        confidence = EXCLUDED.confidence,
        completed_at = EXCLUDED.completed_at;

    INSERT INTO extracted_items (
        id, analysis_id, page_id, item_type, normalized_name, raw_value,
        normalized_value, unit, reference_range, confidence, source_text, created_at
    )
    VALUES
        (
            lab_item_id,
            analysis_id,
            page_id,
            'LAB_VALUE',
            'LDL 콜레스테롤',
            '128',
            '128',
            'mg/dL',
            '0-129',
            0.9600,
            'LDL 콜레스테롤 128 mg/dL',
            TIMESTAMPTZ '2026-09-28 11:02:05+09'
        ),
        (
            medication_item_id,
            analysis_id,
            page_id,
            'MEDICATION',
            '아모디핀',
            '아모디핀 5 mg',
            '아모디핀 5',
            'mg',
            NULL,
            0.9400,
            '아모디핀 5 mg 1일 1회',
            TIMESTAMPTZ '2026-09-28 11:02:06+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        normalized_name = EXCLUDED.normalized_name,
        raw_value = EXCLUDED.raw_value,
        normalized_value = EXCLUDED.normalized_value,
        unit = EXCLUDED.unit,
        source_text = EXCLUDED.source_text;

    INSERT INTO ai_runs (
        id, analysis_id, visit_id, run_type, model_name, prompt_version,
        status, current_step, progress, retryable, retry_count, response_text,
        created_at, completed_at
    )
    VALUES (
        ai_run_id,
        analysis_id,
        visit_id,
        'EXPLANATION',
        'dummy-explanation-model',
        'issue-3-v1',
        'SUCCEEDED',
        'COMPLETED',
        100,
        FALSE,
        0,
        '검사 결과와 복약 정보를 쉽게 설명했습니다.',
        TIMESTAMPTZ '2026-09-28 11:03:00+09',
        TIMESTAMPTZ '2026-09-28 11:03:12+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        current_step = EXCLUDED.current_step,
        progress = EXCLUDED.progress,
        response_text = EXCLUDED.response_text,
        completed_at = EXCLUDED.completed_at;

    INSERT INTO ai_explanations (
        id, document_id, ai_run_id, version, title, content,
        result_status, created_at, completed_at
    )
    VALUES (
        explanation_id,
        document_id,
        ai_run_id,
        1,
        '혈액검사 결과 설명',
        'LDL 콜레스테롤은 현재 참고 범위 안에 있습니다. 처방받은 약은 매일 같은 시간에 복용하고 다음 진료에서 결과를 다시 확인하세요.',
        'COMPLETE',
        TIMESTAMPTZ '2026-09-28 11:03:13+09',
        TIMESTAMPTZ '2026-09-28 11:03:13+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        content = EXCLUDED.content,
        result_status = EXCLUDED.result_status,
        completed_at = EXCLUDED.completed_at;

    INSERT INTO explanation_sections (id, explanation_id, section_type, title, section_order, created_at)
    VALUES
        (
            lab_section_id,
            explanation_id,
            'LAB_RESULT',
            '검사 결과',
            1,
            TIMESTAMPTZ '2026-09-28 11:03:14+09'
        ),
        (
            medication_section_id,
            explanation_id,
            'MEDICATION',
            '복약 안내',
            2,
            TIMESTAMPTZ '2026-09-28 11:03:14+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        section_type = EXCLUDED.section_type,
        title = EXCLUDED.title,
        section_order = EXCLUDED.section_order;

    INSERT INTO explanation_items (
        id, section_id, extracted_item_id, label, display_value, unit,
        has_source, item_order, created_at
    )
    VALUES
        (
            lab_explanation_item_id,
            lab_section_id,
            lab_item_id,
            'LDL 콜레스테롤',
            '128',
            'mg/dL',
            TRUE,
            1,
            TIMESTAMPTZ '2026-09-28 11:03:15+09'
        ),
        (
            medication_explanation_item_id,
            medication_section_id,
            medication_item_id,
            '복용 약',
            '아모디핀 5 mg, 1일 1회',
            NULL,
            TRUE,
            1,
            TIMESTAMPTZ '2026-09-28 11:03:15+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        label = EXCLUDED.label,
        display_value = EXCLUDED.display_value,
        unit = EXCLUDED.unit,
        has_source = EXCLUDED.has_source;

    INSERT INTO explanation_citations (
        id, explanation_id, explanation_item_id, page_id, quoted_text,
        anchor_id, created_at
    )
    VALUES
        (
            lab_citation_id,
            explanation_id,
            lab_explanation_item_id,
            page_id,
            'LDL 콜레스테롤 128 mg/dL',
            'page-1-lab-ldl',
            TIMESTAMPTZ '2026-09-28 11:03:16+09'
        ),
        (
            medication_citation_id,
            explanation_id,
            medication_explanation_item_id,
            page_id,
            '아모디핀 5 mg 1일 1회',
            'page-1-medication',
            TIMESTAMPTZ '2026-09-28 11:03:16+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        quoted_text = EXCLUDED.quoted_text,
        anchor_id = EXCLUDED.anchor_id;
END $$;

COMMIT;
