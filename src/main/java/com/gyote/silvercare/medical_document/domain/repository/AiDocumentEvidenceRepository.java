package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.medical_document.query.model.AiDocumentCitationRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentFactRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentPageRow;
import com.gyote.silvercare.medical_document.domain.entity.AiExplanation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** AI 설명 화면의 원문 근거 조회 전용 저장소입니다. */
public interface AiDocumentEvidenceRepository extends Repository<AiExplanation, UUID> {

    /** 최신 설명 버전에서 원문 추출 항목과 연결된 검증 대상을 조회합니다. */
    @Query(value = """
            SELECT CAST(ei.extracted_item_id AS VARCHAR) AS "factId", ex.item_type AS "factType",
                   ei.display_value AS "displayValue", ex.raw_value AS "originalValue",
                   ei.unit AS "displayUnit", ex.unit AS "originalUnit", ex.source_text AS "sourceText",
                   dp.page_no AS "pageNo", (
                     SELECT ec.anchor_id FROM explanation_citations ec
                     WHERE ec.explanation_item_id = ei.id ORDER BY ec.created_at, ec.id LIMIT 1
                   ) AS "anchorId"
            FROM ai_explanations ae
            JOIN explanation_sections es ON es.explanation_id = ae.id
            JOIN explanation_items ei ON ei.section_id = es.id
            JOIN extracted_items ex ON ex.id = ei.extracted_item_id
            LEFT JOIN document_pages dp ON dp.id = ex.page_id
            WHERE ae.id = (SELECT x.id FROM ai_explanations x WHERE x.document_id = :documentId
                           ORDER BY x.version DESC, x.created_at DESC, x.id DESC LIMIT 1)
            ORDER BY es.section_order, ei.item_order
            """, nativeQuery = true)
    List<AiDocumentFactRow> findFacts(@Param("documentId") UUID documentId);

    /** 최신 설명 버전에서 페이지 위치가 확인된 인용을 조회합니다. */
    @Query(value = """
            SELECT CAST(ec.id AS VARCHAR) AS "citationId", CAST(es.id AS VARCHAR) AS "sectionId",
                   CAST(ei.id AS VARCHAR) AS "sentenceId",
                   CAST(ei.extracted_item_id AS VARCHAR) AS "sourceItemId",
                   CAST(ec.chunk_id AS VARCHAR) AS "chunkId",
                   CAST(COALESCE(ec.page_id, ex.page_id) AS VARCHAR) AS "pageId",
                   dp.page_no AS "pageNo", COALESCE(ec.quoted_text, ex.source_text, dc.content) AS "sourceText",
            CAST(COALESCE(ex.source_box, dc.bounding_box) AS VARCHAR) AS "sourceBox",
                   dp.width_px AS "pageWidthPx", dp.height_px AS "pageHeightPx",
                   ec.anchor_id AS "anchorId"
            FROM explanation_citations ec
            JOIN ai_explanations ae ON ae.id = ec.explanation_id
            LEFT JOIN explanation_items ei ON ei.id = ec.explanation_item_id
            LEFT JOIN explanation_sections es ON es.id = ei.section_id
            LEFT JOIN extracted_items ex ON ex.id = ei.extracted_item_id
            LEFT JOIN document_chunks dc ON dc.id = ec.chunk_id
            LEFT JOIN document_pages dp ON dp.id = COALESCE(ec.page_id, ex.page_id, dc.page_id)
            WHERE ae.id = (SELECT x.id FROM ai_explanations x WHERE x.document_id = :documentId
                           ORDER BY x.version DESC, x.created_at DESC, x.id DESC LIMIT 1)
              AND dp.id IS NOT NULL
              AND (:sectionId IS NULL OR es.id = :sectionId)
            ORDER BY ec.created_at, ec.id
            """, nativeQuery = true)
    List<AiDocumentCitationRow> findCitations(
            @Param("documentId") UUID documentId,
            @Param("sectionId") UUID sectionId
    );

    default List<AiDocumentCitationRow> findCitations(UUID documentId) {
        return findCitations(documentId, null);
    }

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM explanation_citations ec
                JOIN ai_explanations ae ON ae.id = ec.explanation_id
                WHERE ae.document_id = :documentId AND ec.anchor_id = :anchorId
            )
            """, nativeQuery = true)
    boolean existsAnchorInDocument(
            @Param("documentId") UUID documentId,
            @Param("anchorId") String anchorId
    );

    /** 페이지 렌더링 키가 있으면 우선하고, 없으면 원본 문서 키를 반환합니다. */
    @Query(value = """
            SELECT CAST(dp.id AS VARCHAR) AS "pageId", dp.page_no AS "pageNo",
                   COALESCE(NULLIF(dp.rendered_storage_key, ''), d.storage_key) AS "storageKey",
                   CASE WHEN NULLIF(dp.rendered_storage_key, '') IS NOT NULL THEN TRUE ELSE FALSE END AS "renderedPage",
            CAST((
                SELECT COALESCE(ex2.source_box, dc2.bounding_box)
                FROM explanation_citations ec2
                LEFT JOIN explanation_items ei2 ON ei2.id = ec2.explanation_item_id
                LEFT JOIN extracted_items ex2 ON ex2.id = ei2.extracted_item_id
                LEFT JOIN document_chunks dc2 ON dc2.id = ec2.chunk_id
                WHERE ec2.anchor_id = :anchorId
                  AND (ec2.page_id = dp.id OR dc2.page_id = dp.id)
                ORDER BY ec2.created_at, ec2.id
                LIMIT 1
            ) AS VARCHAR) AS "sourceBox",
                   dp.width_px AS "pageWidthPx", dp.height_px AS "pageHeightPx"
            FROM document_pages dp JOIN documents d ON d.id = dp.document_id
            WHERE dp.document_id = :documentId
              AND dp.page_no = :pageNo
              AND d.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<AiDocumentPageRow> findPage(
            @Param("documentId") UUID documentId,
            @Param("pageNo") int pageNo,
            @Param("anchorId") String anchorId
    );

    default Optional<AiDocumentPageRow> findPage(UUID documentId, int pageNo) {
        return findPage(documentId, pageNo, null);
    }
}
