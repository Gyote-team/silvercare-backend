package com.gyote.silvercare.health_record.domain.repository;

import com.gyote.silvercare.health_record.domain.entity.HealthRecord;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 삭제 상태와 대상 개인을 기준으로 건강기록을 조회한다. */
public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    /** 삭제되지 않은 기록만 상세 조회한다. */
    Optional<HealthRecord> findByIdAndDeletedAtIsNull(UUID id);

    /** 첫 페이지는 null 커서 파라미터 없이 조회한다. */
    @Query(
            "select r from HealthRecord r where r.patientId=:patientId and r.deletedAt is null"
                + " order by r.createdAt desc, r.id desc")
    List<HealthRecord> findFirstPage(@Param("patientId") UUID patientId, Pageable pageable);

    /** 시각과 UUID를 함께 비교해 같은 시각의 기록도 빠짐없이 조회한다. */
    @Query(
            "select r from HealthRecord r where r.patientId=:patientId and r.deletedAt is null and "
                    + "(r.createdAt < :before or (r.createdAt = :before and r.id < :beforeId)) "
                    + "order by r.createdAt desc, r.id desc")
    List<HealthRecord> findPage(
            @Param("patientId") UUID patientId,
            @Param("before") Instant before,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);
}
