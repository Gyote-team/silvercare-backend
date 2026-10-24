package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    /** 분석이 완료된 문서를 최신 생성 순으로 조회한다. */
    @Query(value = """
            SELECT
                CAST(d.id AS VARCHAR) AS "documentId",
                d.file_name AS "documentName",
                d.document_type AS "documentType",
                CAST(d.visit_id AS VARCHAR) AS "visitId",
                v.visited_on AS "visitedOn",
                CAST(d.created_at AS VARCHAR) AS "createdAt",
                uploader.name AS "authorName",
                d.status AS "documentStatus",
                COALESCE(
                    latest_run.status,
                    CASE latest_analysis.status
                        WHEN 'RUNNING' THEN 'RUNNING'
                        WHEN 'FAILED' THEN 'FAILED'
                        ELSE 'QUEUED'
                    END
                ) AS "jobStatus",
                latest_explanation.result_status AS "resultStatus"
            FROM documents d
            JOIN visits v ON v.id = d.visit_id
            JOIN users uploader ON uploader.id = d.uploader_user_id
            JOIN document_analyses latest_analysis
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
            WHERE v.patient_id = :patientId
              AND d.deleted_at IS NULL
              AND latest_analysis.status = 'SUCCEEDED'
              AND (:visitId IS NULL OR d.visit_id = :visitId)
              AND (:documentType IS NULL OR d.document_type = :documentType)
              AND (:documentStatus IS NULL OR d.status = :documentStatus)
            ORDER BY d.created_at DESC, d.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM documents d
            JOIN visits v ON v.id = d.visit_id
            JOIN document_analyses latest_analysis
                ON latest_analysis.id = (
                    SELECT da.id
                    FROM document_analyses da
                    WHERE da.document_id = d.id
                    ORDER BY da.created_at DESC, da.id DESC
                    LIMIT 1
                )
            WHERE v.patient_id = :patientId
              AND d.deleted_at IS NULL
              AND latest_analysis.status = 'SUCCEEDED'
              AND (:visitId IS NULL OR d.visit_id = :visitId)
              AND (:documentType IS NULL OR d.document_type = :documentType)
              AND (:documentStatus IS NULL OR d.status = :documentStatus)
            """,
            nativeQuery = true)
    Page<AiDocumentListRow> findCompletedDocuments(
            @Param("patientId") UUID patientId,
            @Param("visitId") UUID visitId,
            @Param("documentType") String documentType,
            @Param("documentStatus") String documentStatus,
            Pageable pageable
    );

    /** 문서 식별자로 문서가 속한 환자를 조회한다. */
    @Query(value = """
            SELECT CAST(v.patient_id AS VARCHAR) AS "patientId"
            FROM documents d
            JOIN visits v ON v.id = d.visit_id
            WHERE d.id = :documentId
              AND d.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<AiDocumentPatientRow> findPatientByDocumentId(@Param("documentId") UUID documentId);

    /** 문서-환자 조회 결과를 담는 프로젝션이다. */
    interface AiDocumentPatientRow {
        /** 환자 식별자를 반환한다. */
        String getPatientId();
    }
}
