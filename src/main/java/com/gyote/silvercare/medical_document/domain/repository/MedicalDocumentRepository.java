package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentAiStatusRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 의료 문서 Repository입니다. 문서 목록·상세 조회와 업로드에 필요한 멱등 키 조회·방문의 환자 조회를 담당합니다.
 * 목록은 (createdAt, id) 내림차순 커서 방식이며, excluded에는 DocumentStatus.DELETED를 넘깁니다.
 * visitId 필터 유무에 따라 메서드를 나눈 이유는 null 파라미터 타입 추론 문제를 피하기 위해서입니다.
 */
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    Optional<MedicalDocument> findByIdAndStatusNot(UUID id, DocumentStatus excluded);

    /** 업로드한 사용자와 Idempotency-Key가 같은 문서를 찾아 반환합니다. 삭제된 문서도 포함합니다. */
    Optional<MedicalDocument> findByUploaderUserIdAndIdempotencyKey(UUID uploaderUserId, String idempotencyKey);

    /** 삭제되지 않은 방문의 patient_id를 문자열로 반환합니다. 방문이 없거나 삭제됐으면 빈 Optional입니다. */
    @Query(value = """
            SELECT CAST(v.patient_id AS VARCHAR)
            FROM visits v
            WHERE v.id = :visitId
              AND v.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<String> findPatientIdByVisitId(@Param("visitId") UUID visitId);

    /** 첫 페이지 (방문 필터 없음) */
    @Query("""
            select d from MedicalDocument d
            where d.patientId = :patientId
              and d.status <> :excluded
            order by d.createdAt desc, d.id desc
            """)
    List<MedicalDocument> findFirstPage(
            @Param("patientId") UUID patientId,
            @Param("excluded") DocumentStatus excluded,
            Pageable pageable
    );

    /** 첫 페이지 (방문 필터 있음) */
    @Query("""
            select d from MedicalDocument d
            where d.patientId = :patientId
              and d.visitId = :visitId
              and d.status <> :excluded
            order by d.createdAt desc, d.id desc
            """)
    List<MedicalDocument> findFirstPageByVisit(
            @Param("patientId") UUID patientId,
            @Param("visitId") UUID visitId,
            @Param("excluded") DocumentStatus excluded,
            Pageable pageable
    );

    /** 다음 페이지 (방문 필터 없음). 커서 = 이전 페이지 마지막 문서의 (createdAt, id) */
    @Query("""
            select d from MedicalDocument d
            where d.patientId = :patientId
              and d.status <> :excluded
              and (d.createdAt < :cursorCreatedAt
                   or (d.createdAt = :cursorCreatedAt and d.id < :cursorId))
            order by d.createdAt desc, d.id desc
            """)
    List<MedicalDocument> findNextPage(
            @Param("patientId") UUID patientId,
            @Param("excluded") DocumentStatus excluded,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /** 다음 페이지 (방문 필터 있음) */
    @Query("""
            select d from MedicalDocument d
            where d.patientId = :patientId
              and d.visitId = :visitId
              and d.status <> :excluded
              and (d.createdAt < :cursorCreatedAt
                   or (d.createdAt = :cursorCreatedAt and d.id < :cursorId))
            order by d.createdAt desc, d.id desc
            """)
    List<MedicalDocument> findNextPageByVisit(
            @Param("patientId") UUID patientId,
            @Param("visitId") UUID visitId,
            @Param("excluded") DocumentStatus excluded,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /** 문서별 방문일과 최신 AI 설명 작업 상태를 한 번에 조회한다. 분석 전 문서도 포함한다. */
    @Query(value = """
            SELECT
                CAST(d.id AS VARCHAR) AS "documentId",
                v.visited_on AS "visitedOn",
                CASE
                    WHEN latest_analysis.id IS NULL THEN NULL
                    ELSE COALESCE(
                        latest_run.status,
                        CASE latest_analysis.status
                            WHEN 'RUNNING' THEN 'RUNNING'
                            WHEN 'FAILED' THEN 'FAILED'
                            ELSE 'QUEUED'
                        END
                    )
                END AS "jobStatus",
                latest_explanation.result_status AS "resultStatus",
                COALESCE(latest_run.retryable, FALSE) AS "retryable"
            FROM documents d
            LEFT JOIN visits v ON v.id = d.visit_id
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
            WHERE d.id IN (:documentIds)
            """, nativeQuery = true)
    List<MedicalDocumentAiStatusRow> findAiStatusesByDocumentIds(
            @Param("documentIds") Collection<UUID> documentIds
    );
}