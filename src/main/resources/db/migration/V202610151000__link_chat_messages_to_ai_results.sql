ALTER TABLE chat_messages ADD COLUMN result_document_id UUID;
ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_result_document_fk
    FOREIGN KEY (result_document_id) REFERENCES ai_result_documents (id);
