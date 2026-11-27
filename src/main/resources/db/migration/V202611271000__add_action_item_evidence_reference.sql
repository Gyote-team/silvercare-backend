-- 과거 후보는 근거를 추정하지 않고 NULL로 유지한다.
ALTER TABLE action_items
    ADD COLUMN source_item_id UUID REFERENCES extracted_items(id) ON DELETE SET NULL,
    ADD COLUMN confidence NUMERIC(5, 4),
    ADD CONSTRAINT action_items_confidence_check
        CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1);

CREATE INDEX action_items_document_created_idx
    ON action_items(document_id, created_at DESC, id DESC);
