package com.gyote.silvercare.previsit_summary.domain.repository;

import com.gyote.silvercare.previsit_summary.domain.entity.PrevisitSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

/** 요청 멱등성과 실행 선점·실패 복구를 위한 저장소. */
public interface PrevisitSummaryRepository extends JpaRepository<PrevisitSummary, UUID> {
    Optional<PrevisitSummary> findByRequestedByUserIdAndIdempotencyKey(UUID actor, String key);
    Optional<PrevisitSummary> findBySummaryId(UUID summaryId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PrevisitSummary r where r.id=:id")
    Optional<PrevisitSummary> findLocked(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PrevisitSummary r where r.jobStatus='QUEUED' order by r.createdAt,r.id")
    List<PrevisitSummary> findQueued(Pageable page);
    @Query("select r from PrevisitSummary r where r.jobStatus='RUNNING' and r.startedAt<:cutoff")
    List<PrevisitSummary> findExpired(@Param("cutoff") Instant cutoff, Pageable page);
}
