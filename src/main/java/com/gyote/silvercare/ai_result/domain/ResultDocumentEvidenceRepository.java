package com.gyote.silvercare.ai_result.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResultDocumentEvidenceRepository extends Repository<AiResultDocument, UUID> {

    @Query(value = """
            SELECT CAST(ei.id AS VARCHAR) AS "itemId", CAST(d.id AS VARCHAR) AS "documentId",
                   dp.page_no AS "pageNo", ei.normalized_name AS "label", ei.raw_value AS "rawValue",
                   ei.unit AS "unit", ei.reference_range AS "referenceRange", ei.source_text AS "sourceText"
            FROM extracted_items ei
            JOIN document_analyses da ON da.id = ei.analysis_id AND da.status = 'SUCCEEDED'
            JOIN documents d ON d.id = da.document_id
            LEFT JOIN document_pages dp ON dp.id = ei.page_id
            WHERE d.patient_id = :patientId
              AND d.id IN (:documentIds)
              AND d.deleted_at IS NULL
            ORDER BY d.created_at DESC, ei.created_at DESC
            LIMIT 40
            """, nativeQuery = true)
    List<EvidenceRow> findEvidence(@Param("patientId") UUID patientId, @Param("documentIds") List<UUID> documentIds);

    interface EvidenceRow {
        String getItemId();
        String getDocumentId();
        Integer getPageNo();
        String getLabel();
        String getRawValue();
        String getUnit();
        String getReferenceRange();
        String getSourceText();
    }
}
