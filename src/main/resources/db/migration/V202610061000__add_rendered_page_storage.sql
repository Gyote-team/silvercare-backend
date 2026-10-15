-- 페이지 단위 근거 화면을 위한 렌더링 산출물 메타데이터입니다.
-- 기존 문서는 NULL로 남겨 원본 문서 URL 폴백을 유지합니다.
ALTER TABLE document_pages
    ADD COLUMN rendered_storage_key VARCHAR(500),
    ADD COLUMN rendered_mime_type VARCHAR(100),
    ADD COLUMN rendered_at TIMESTAMPTZ(6);

ALTER TABLE document_pages
    ADD CONSTRAINT document_pages_rendered_asset_check CHECK (
        (rendered_storage_key IS NULL AND rendered_mime_type IS NULL AND rendered_at IS NULL)
        OR (rendered_storage_key IS NOT NULL AND rendered_mime_type IS NOT NULL AND rendered_at IS NOT NULL)
    );

CREATE INDEX pages_rendered_storage_key_idx
    ON document_pages (rendered_storage_key)
    WHERE rendered_storage_key IS NOT NULL;
