-- 의료 문서 메타데이터. 원본은 Object Storage에 두고 키만 저장한다.
CREATE TABLE medical_documents (
       id UUID NOT NULL,
       patient_id UUID NOT NULL,
       visit_id UUID,
       uploader_id UUID NOT NULL,
       document_name VARCHAR(255) NOT NULL,
       object_key VARCHAR(512) NOT NULL,
       mime_type VARCHAR(100) NOT NULL,
       request_id VARCHAR(64),
       document_type VARCHAR(30) NOT NULL,
       document_status VARCHAR(20) NOT NULL,
       status_changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
       created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
       updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
       deleted_at TIMESTAMP(6) WITH TIME ZONE,
       PRIMARY KEY (id),
       CONSTRAINT medical_documents_status_check CHECK (
           document_status IN ('UPLOADED', 'PROCESSING', 'READY', 'NEEDS_REVIEW', 'FAILED', 'DELETED')
           ),
       CONSTRAINT medical_documents_type_check CHECK (
           document_type IN ('LAB_RESULT', 'PRESCRIPTION', 'DIAGNOSIS', 'DISCHARGE_GUIDE', 'UNKNOWN')
           ),
       CONSTRAINT medical_documents_patient_fk FOREIGN KEY (patient_id) REFERENCES users (id),
       CONSTRAINT medical_documents_uploader_fk FOREIGN KEY (uploader_id) REFERENCES users (id)
);

CREATE INDEX medical_documents_patient_created_idx
    ON medical_documents (patient_id, created_at, id);
CREATE INDEX medical_documents_visit_idx
    ON medical_documents (visit_id);