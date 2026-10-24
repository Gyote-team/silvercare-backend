-- 같은 Idempotency-Key로 다른 파일이 올라왔는지 가려내려고 파일 내용의 SHA-256을 저장한다. 기존 행은 NULL로 둔다.
ALTER TABLE documents
    ADD COLUMN content_sha256 VARCHAR(64);
