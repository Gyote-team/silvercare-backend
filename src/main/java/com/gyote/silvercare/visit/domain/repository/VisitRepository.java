package com.gyote.silvercare.visit.domain.repository;

import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 삭제 여부·대상·취소 필터와 날짜/UUID 커서로 방문을 조회한다. */
public interface VisitRepository extends JpaRepository<Visit, UUID> {
    /** 조회 가능한 방문 상세를 반환한다. */
    Optional<Visit> findByIdAndDeletedAtIsNull(UUID id);

    /** 첫 페이지는 nullable 커서 없이 조회한다. */
    @Query(
            "select v from Visit v where v.patientId=:patientId and v.deletedAt is null and"
                + " (:includeCanceled=true or v.status<>:canceled) order by v.visitedOn desc,v.id"
                + " desc")
    List<Visit> findFirstPage(
            @Param("patientId") UUID patientId,
            @Param("includeCanceled") boolean includeCanceled,
            @Param("canceled") VisitStatus canceled,
            Pageable pageable);

    /** 같은 날짜도 UUID로 구분해 누락 없이 조회한다. */
    @Query(
            "select v from Visit v where v.patientId=:patientId and v.deletedAt is null and"
                + " (:includeCanceled=true or v.status<>:canceled) and (v.visitedOn<:before or"
                + " (v.visitedOn=:before and v.id<:beforeId)) order by v.visitedOn desc,v.id desc")
    List<Visit> findPage(
            @Param("patientId") UUID patientId,
            @Param("includeCanceled") boolean includeCanceled,
            @Param("canceled") VisitStatus canceled,
            @Param("before") LocalDate before,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);
}
