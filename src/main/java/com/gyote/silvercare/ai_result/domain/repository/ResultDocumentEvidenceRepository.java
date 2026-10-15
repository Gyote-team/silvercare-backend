package com.gyote.silvercare.ai_result.domain.repository;

import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResultDocumentEvidenceRepository extends Repository<AiResultDocument, UUID> {

    /** 선택한 문서에서 분석이 완료된 최신 원문 근거를 조회한다. */
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
        /** 추출 항목 식별자를 반환한다. */
        String getItemId();

        /** 근거가 포함된 문서 식별자를 반환한다. */
        String getDocumentId();

        /** 근거가 위치한 문서 페이지 번호를 반환한다. */
        Integer getPageNo();

        /** 추출 항목의 정규화된 이름을 반환한다. */
        String getLabel();

        /** 추출 항목의 원본 값을 반환한다. */
        String getRawValue();

        /** 추출 값의 단위를 반환한다. */
        String getUnit();

        /** 추출 값의 참고 범위를 반환한다. */
        String getReferenceRange();

        /** AI 답변 근거로 사용할 원문을 반환한다. */
        String getSourceText();
    }
}
