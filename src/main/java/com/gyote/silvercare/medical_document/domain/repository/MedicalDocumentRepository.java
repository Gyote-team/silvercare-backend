package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 의료 문서 조회용 Repository입니다.
 * 목록은 (createdAt, id) 내림차순 커서 방식이며, excluded에는 DocumentStatus.DELETED를 넘깁니다.
 * visitId 필터 유무에 따라 메서드를 나눈 이유는 null 파라미터 타입 추론 문제를 피하기 위해서입니다.
 */
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    Optional<MedicalDocument> findByIdAndStatusNot(UUID id, DocumentStatus excluded);

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
}