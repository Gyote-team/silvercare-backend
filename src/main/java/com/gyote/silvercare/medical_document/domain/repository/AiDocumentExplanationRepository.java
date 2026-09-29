package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.medical_document.domain.entity.AiExplanation;
import com.gyote.silvercare.medical_document.query.model.AiDocumentDetailRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentStatusRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiDocumentExplanationRepository extends JpaRepository<AiExplanation, UUID> {

    /** 문서의 최신 버전 AI 설명 엔티티를 조회한다. */
    Optional<AiExplanation> findTopByDocumentIdOrderByVersionDesc(UUID documentId);

    /** 문서 상세 화면에 필요한 최신 분석 및 설명 정보를 조회한다. */
    @Query(value = """
            SELECT
                CAST(d.id AS VARCHAR) AS "documentId",
                d.file_name AS "documentName",
                d.document_type AS "documentType",
                CAST(d.visit_id AS VARCHAR) AS "visitId",
                d.status AS "documentStatus",
                latest_analysis.status AS "analysisStatus",
                COALESCE(
                    latest_run.status,
                    CASE latest_analysis.status
                        WHEN 'RUNNING' THEN 'RUNNING'
                        WHEN 'FAILED' THEN 'FAILED'
                        ELSE 'QUEUED'
                    END
                ) AS "jobStatus",
                latest_explanation.result_status AS "resultStatus",
                latest_explanation.title AS "title",
                latest_explanation.content AS "content",
                CAST(d.created_at AS VARCHAR) AS "createdAt",
                COALESCE((
                    SELECT COUNT(*)
                    FROM explanation_sections es
                    WHERE es.explanation_id = latest_explanation.id
                ), 0) AS "sectionCount",
                COALESCE((
                    SELECT COUNT(*)
                    FROM explanation_citations ec
                    WHERE ec.explanation_id = latest_explanation.id
                ), 0) AS "citationCount"
            FROM documents d
            LEFT JOIN document_analyses latest_analysis
                ON latest_analysis.id = (
                    SELECT da.id
                    FROM document_analyses da
                    WHERE da.document_id = d.id
                    ORDER BY da.created_at DESC, da.id DESC
                    LIMIT 1
                )
            LEFT JOIN ai_runs latest_run
                ON latest_run.id = (
                    SELECT ar.id
                    FROM ai_runs ar
                    WHERE ar.analysis_id = latest_analysis.id
                      AND ar.run_type = 'EXPLANATION'
                    ORDER BY ar.created_at DESC, ar.id DESC
                    LIMIT 1
                )
            LEFT JOIN ai_explanations latest_explanation
                ON latest_explanation.id = (
                    SELECT ae.id
                    FROM ai_explanations ae
                    WHERE ae.document_id = d.id
                    ORDER BY ae.version DESC, ae.created_at DESC, ae.id DESC
                    LIMIT 1
                )
            WHERE d.id = :documentId
              AND d.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<AiDocumentDetailRow> findDetailByDocumentId(@Param("documentId") UUID documentId);

    /** 문서의 최신 AI 설명 작업 상태를 조회한다. */
    @Query(value = """
            SELECT
                CAST(d.id AS VARCHAR) AS "documentId",
                latest_analysis.status AS "analysisStatus",
                COALESCE(
                    latest_run.status,
                    CASE latest_analysis.status
                        WHEN 'RUNNING' THEN 'RUNNING'
                        WHEN 'FAILED' THEN 'FAILED'
                        ELSE 'QUEUED'
                    END
                ) AS "jobStatus",
                latest_explanation.result_status AS "resultStatus",
                latest_run.current_step AS "currentStep",
                latest_run.progress AS "progress",
                CAST(latest_run.completed_at AS VARCHAR) AS "completedAt",
                latest_run.failed_step AS "failedStep",
                latest_run.error_code AS "errorCode",
                latest_run.retryable AS "retryable"
            FROM documents d
            LEFT JOIN document_analyses latest_analysis
                ON latest_analysis.id = (
                    SELECT da.id
                    FROM document_analyses da
                    WHERE da.document_id = d.id
                    ORDER BY da.created_at DESC, da.id DESC
                    LIMIT 1
                )
            LEFT JOIN ai_runs latest_run
                ON latest_run.id = (
                    SELECT ar.id
                    FROM ai_runs ar
                    WHERE ar.analysis_id = latest_analysis.id
                      AND ar.run_type = 'EXPLANATION'
                    ORDER BY ar.created_at DESC, ar.id DESC
                    LIMIT 1
                )
            LEFT JOIN ai_explanations latest_explanation
                ON latest_explanation.id = (
                    SELECT ae.id
                    FROM ai_explanations ae
                    WHERE ae.document_id = d.id
                    ORDER BY ae.version DESC, ae.created_at DESC, ae.id DESC
                    LIMIT 1
                )
            WHERE d.id = :documentId
              AND d.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<AiDocumentStatusRow> findStatusByDocumentId(@Param("documentId") UUID documentId);

    /** 문서의 설명 섹션과 항목 및 대표 인용을 조회한다. */
    @Query(value = """
            SELECT
                CAST(d.id AS VARCHAR) AS "documentId",
                CAST(es.id AS VARCHAR) AS "sectionId",
                es.section_type AS "sectionType",
                es.title AS "sectionTitle",
                es.section_order AS "sectionOrder",
                CAST(ei.id AS VARCHAR) AS "sentenceId",
                CAST(ei.extracted_item_id AS VARCHAR) AS "sourceItemId",
                ei.label AS "label",
                ei.display_value AS "displayValue",
                ei.unit AS "unit",
                COALESCE(ei.has_source, FALSE) AS "hasSource",
                (
                    SELECT CAST(ec.id AS VARCHAR)
                    FROM explanation_citations ec
                    WHERE ec.explanation_item_id = ei.id
                    ORDER BY ec.created_at ASC, ec.id ASC
                    LIMIT 1
                ) AS "citationId"
            FROM documents d
            JOIN ai_explanations latest_explanation
                ON latest_explanation.id = (
                    SELECT ae.id
                    FROM ai_explanations ae
                    WHERE ae.document_id = d.id
                    ORDER BY ae.version DESC, ae.created_at DESC, ae.id DESC
                    LIMIT 1
                )
            JOIN explanation_sections es ON es.explanation_id = latest_explanation.id
            LEFT JOIN explanation_items ei ON ei.section_id = es.id
            WHERE d.id = :documentId
              AND d.deleted_at IS NULL
              AND (:sectionType IS NULL OR es.section_type = :sectionType)
            ORDER BY es.section_order ASC, ei.item_order ASC
            """, nativeQuery = true)
    List<AiDocumentSectionRow> findSectionRows(
            @Param("documentId") UUID documentId,
            @Param("sectionType") String sectionType
    );
}
