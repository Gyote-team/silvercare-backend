-- 소견서 AI 의료문서 조회 화면 확인용 더미 데이터
--
-- 실행:
--   docker exec -i silvercare-postgres-1 psql -U silvercare -d silvercare < dummydata/issue-4-opinion-document.sql
--
-- demo-patient / demo-caregiver 사용자가 없으면 로컬 확인용 계정을 함께 만든다.
-- 고정 UUID를 사용하므로 같은 파일을 여러 번 실행해도 동일한 데이터만 갱신한다.

BEGIN;

-- 기존 혈액검사 더미 문서와 하위 분석 데이터를 제거한다.
DELETE FROM explanation_citations WHERE explanation_id = '88000000-0000-0000-0000-000000000001';
DELETE FROM explanation_items WHERE section_id IN ('99000000-0000-0000-0000-000000000001', '99000000-0000-0000-0000-000000000002');
DELETE FROM explanation_sections WHERE explanation_id = '88000000-0000-0000-0000-000000000001';
DELETE FROM ai_explanations WHERE id = '88000000-0000-0000-0000-000000000001';
DELETE FROM ai_runs WHERE id = '77000000-0000-0000-0000-000000000001';
DELETE FROM extracted_items WHERE analysis_id = '44000000-0000-0000-0000-000000000001';
DELETE FROM document_chunks WHERE analysis_id = '44000000-0000-0000-0000-000000000001';
DELETE FROM document_pages WHERE document_id = '33000000-0000-0000-0000-000000000001';
DELETE FROM document_analyses WHERE id = '44000000-0000-0000-0000-000000000001';
DELETE FROM documents WHERE id = '33000000-0000-0000-0000-000000000001';

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
    af_item_id UUID := '66000000-0000-0000-0000-000000000003';
    fever_item_id UUID := '66000000-0000-0000-0000-000000000004';
    prostate_item_id UUID := '66000000-0000-0000-0000-000000000005';
    ai_run_id UUID := '77000000-0000-0000-0000-000000000001';
    explanation_id UUID := '88000000-0000-0000-0000-000000000001';
    lab_section_id UUID := '99000000-0000-0000-0000-000000000001';
    medication_section_id UUID := '99000000-0000-0000-0000-000000000002';
    lab_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000001';
    medication_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000002';
    af_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000003';
    fever_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000004';
    prostate_explanation_item_id UUID := 'aa000000-0000-0000-0000-000000000005';
    lab_citation_id UUID := 'bb000000-0000-0000-0000-000000000001';
    medication_citation_id UUID := 'bb000000-0000-0000-0000-000000000002';
    af_citation_id UUID := 'bb000000-0000-0000-0000-000000000003';
    fever_citation_id UUID := 'bb000000-0000-0000-0000-000000000004';
    prostate_citation_id UUID := 'bb000000-0000-0000-0000-000000000005';
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
        '종합병원',
        '내과',
        DATE '2024-10-29',
        'COMPLETED',
        '소견서 발급 및 향후 치료계획 확인',
        TIMESTAMPTZ '2024-10-29 10:00:00+09',
        TIMESTAMPTZ '2024-10-29 10:00:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        patient_id = EXCLUDED.patient_id,
        hospital_name = EXCLUDED.hospital_name,
        department = EXCLUDED.department,
        visited_on = EXCLUDED.visited_on,
        status = EXCLUDED.status,
        memo = EXCLUDED.memo;

    INSERT INTO documents (
        id, visit_id, patient_id, uploader_user_id, storage_key, file_name, mime_type,
        file_size_bytes, document_type, status, created_at, updated_at
    )
    VALUES (
        document_id,
        visit_id,
        patient_id,
        patient_user_id,
        'dummy/issue-4/opinion-2024-10-29.pdf',
        '2024-10-29_소견서.pdf',
        'application/pdf',
        184320,
        'DIAGNOSIS',
        'READY',
        TIMESTAMPTZ '2024-10-29 11:00:00+09',
        TIMESTAMPTZ '2024-10-29 11:00:00+09'
    )
    ON CONFLICT (id) DO UPDATE SET
        visit_id = EXCLUDED.visit_id,
        uploader_user_id = EXCLUDED.uploader_user_id,
        file_name = EXCLUDED.file_name,
        document_type = EXCLUDED.document_type,
        status = EXCLUDED.status,
        deleted_at = NULL;

    INSERT INTO document_pages (
        id, document_id, page_no, ocr_status, ocr_text, width_px, height_px,
        rendered_storage_key, rendered_mime_type, rendered_at, created_at
    )
    VALUES (
        page_id,
        document_id,
        1,
        'SUCCEEDED',
        E'E87.1 저삼투압 및 저나트륨혈증\nI48.0 발작성 심방세동\nR50.99 발열\nN40.3 전립선비대 및 요로폐색',
        600,
        800,
        'documents/55000000-0000-0000-0000-000000000001/pages/1.png',
        'image/png',
        TIMESTAMPTZ '2024-10-29 11:01:00+09',
        TIMESTAMPTZ '2024-10-29 11:01:00+09'
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
        'dummy-ocr-1.0',
        'dummy-explanation-1.0',
        'SUCCEEDED',
        0.9500,
        TIMESTAMPTZ '2024-10-29 11:02:00+09',
        TIMESTAMPTZ '2024-10-29 11:02:08+09',
        TIMESTAMPTZ '2024-10-29 11:02:00+09'
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
            'DIAGNOSIS_TEXT',
            '저나트륨혈증',
            '혈액 속 나트륨 수치가 낮은 상태예요.',
            'E87.1 저삼투압 및 저나트륨혈증',
            NULL,
            NULL,
            0.9600,
            'E87.1 저삼투압 및 저나트륨혈증',
            TIMESTAMPTZ '2024-10-29 11:02:05+09'
        ),
        (
            af_item_id, analysis_id, page_id, 'DIAGNOSIS_TEXT', '발작성 심방세동',
            '맥박이 불규칙해지는 심장 리듬 이상이에요.', 'I48.0 발작성 심방세동', NULL, NULL,
            0.9500, 'I48.0 발작성 심방세동', TIMESTAMPTZ '2024-10-29 11:02:05+09'
        ),
        (
            fever_item_id, analysis_id, page_id, 'DIAGNOSIS_TEXT', '발열',
            '체온이 정상보다 높아진 상태예요.', 'R50.99 발열', NULL, NULL,
            0.9500, 'R50.99 발열', TIMESTAMPTZ '2024-10-29 11:02:05+09'
        ),
        (
            prostate_item_id, analysis_id, page_id, 'DIAGNOSIS_TEXT', '전립선비대 및 요로폐색',
            '전립선이 커져 소변 흐름에 불편이 생길 수 있어요.', 'N40.3 전립선비대 및 요로폐색', NULL, NULL,
            0.9500, 'N40.3 전립선비대 및 요로폐색', TIMESTAMPTZ '2024-10-29 11:02:05+09'
        ),
        (
            medication_item_id,
            analysis_id,
            page_id,
            'CAUTION',
            '환자 상태 및 치료 의견',
            '저나트륨혈증 추적검사와 CRP 증가에 대한 추적관리가 필요합니다.',
            '저나트륨혈증 추적검사, CRP 추적관리',
            NULL,
            NULL,
            0.9400,
            '저나트륨혈증 추적검사와 CRP 증가에 대한 추적관리가 필요합니다.',
            TIMESTAMPTZ '2024-10-29 11:02:06+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        normalized_name = EXCLUDED.normalized_name,
        raw_value = EXCLUDED.raw_value,
        normalized_value = EXCLUDED.normalized_value,
        unit = EXCLUDED.unit,
        source_text = EXCLUDED.source_text;

    -- 첨부된 소견서 이미지 기준의 시연용 원문 위치입니다.
    UPDATE extracted_items SET source_box = '{"x":104,"y":196,"width":392,"height":124}'::jsonb WHERE id = lab_item_id;
    UPDATE extracted_items SET source_box = '{"x":104,"y":196,"width":392,"height":124}'::jsonb WHERE id = af_item_id;
    UPDATE extracted_items SET source_box = '{"x":104,"y":196,"width":392,"height":124}'::jsonb WHERE id = fever_item_id;
    UPDATE extracted_items SET source_box = '{"x":104,"y":196,"width":392,"height":124}'::jsonb WHERE id = prostate_item_id;
    UPDATE extracted_items SET source_box = '{"x":104,"y":322,"width":392,"height":112}'::jsonb WHERE id = medication_item_id;

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
        '소견서의 진단명과 환자 상태를 쉽게 설명했습니다.',
        TIMESTAMPTZ '2024-10-29 11:03:00+09',
        TIMESTAMPTZ '2024-10-29 11:03:12+09'
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
        '소견서 내용 설명',
        '74세 남성의 소견서입니다. 저나트륨혈증, 발작성 심방세동, 발열, 전립선비대 및 요로폐색이 기재되어 있습니다. 저나트륨혈증과 CRP 증가에 대한 추적관리가 필요하다는 의견입니다.',
        'COMPLETE',
        TIMESTAMPTZ '2024-10-29 11:03:13+09',
        TIMESTAMPTZ '2024-10-29 11:03:13+09'
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
            '진단명',
            1,
            TIMESTAMPTZ '2024-10-29 11:03:14+09'
        ),
        (
            medication_section_id,
            explanation_id,
            'CAUTION',
            '환자 상태 및 향후 치료 의견',
            2,
            TIMESTAMPTZ '2024-10-29 11:03:14+09'
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
            '저나트륨혈증',
            '혈액 속 나트륨 수치가 낮은 상태예요.',
            NULL,
            TRUE,
            1,
            TIMESTAMPTZ '2024-10-29 11:03:15+09'
        ),
        (
            af_explanation_item_id, lab_section_id, af_item_id, '발작성 심방세동',
            '맥박이 불규칙해지는 심장 리듬 이상이에요.', NULL, TRUE, 2,
            TIMESTAMPTZ '2024-10-29 11:03:15+09'
        ),
        (
            fever_explanation_item_id, lab_section_id, fever_item_id, '발열',
            '체온이 정상보다 높아진 상태예요.', NULL, TRUE, 3,
            TIMESTAMPTZ '2024-10-29 11:03:15+09'
        ),
        (
            prostate_explanation_item_id, lab_section_id, prostate_item_id, '전립선비대 및 요로폐색',
            '전립선이 커져 소변 흐름에 불편이 생길 수 있어요.', NULL, TRUE, 4,
            TIMESTAMPTZ '2024-10-29 11:03:15+09'
        ),
        (
            medication_explanation_item_id,
            medication_section_id,
            medication_item_id,
            '추적관리',
            '74세 남성. 저나트륨혈증 추적검사 및 CRP 증가 추적관리 필요',
            NULL,
            TRUE,
            1,
            TIMESTAMPTZ '2024-10-29 11:03:15+09'
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
            E'E87.1 저삼투압 및 저나트륨혈증\nI48.0 발작성 심방세동\nR50.99 발열\nN40.3 전립선비대 및 요로폐색',
            'page-1-diagnosis',
            TIMESTAMPTZ '2024-10-29 11:03:16+09'
        ),
        (
            af_citation_id, explanation_id, af_explanation_item_id, page_id,
            'I48.0 발작성 심방세동', 'page-1-atrial-fibrillation',
            TIMESTAMPTZ '2024-10-29 11:03:16+09'
        ),
        (
            fever_citation_id, explanation_id, fever_explanation_item_id, page_id,
            'R50.99 발열', 'page-1-fever',
            TIMESTAMPTZ '2024-10-29 11:03:16+09'
        ),
        (
            prostate_citation_id, explanation_id, prostate_explanation_item_id, page_id,
            'N40.3 전립선비대 및 요로폐색', 'page-1-prostate',
            TIMESTAMPTZ '2024-10-29 11:03:16+09'
        ),
        (
            medication_citation_id,
            explanation_id,
            medication_explanation_item_id,
            page_id,
            '저나트륨혈증 추적검사와 CRP 증가에 대한 추적관리가 필요합니다.',
            'page-1-follow-up',
            TIMESTAMPTZ '2024-10-29 11:03:16+09'
        )
    ON CONFLICT (id) DO UPDATE SET
        quoted_text = EXCLUDED.quoted_text,
        anchor_id = EXCLUDED.anchor_id;
END $$;

COMMIT;
