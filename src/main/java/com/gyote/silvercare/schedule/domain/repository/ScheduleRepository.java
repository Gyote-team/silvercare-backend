package com.gyote.silvercare.schedule.domain.repository;

import com.gyote.silvercare.schedule.domain.entity.Schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    /** 삭제되지 않은 일정 한 건을 반환한다. */
    Optional<Schedule> findByIdAndDeletedAtIsNull(UUID id);

    /** 대상 개인의 삭제되지 않은 반복 규칙을 페이지로 조회한다. */
    Page<Schedule> findByPatientIdAndDeletedAtIsNull(UUID patientId, Pageable pageable);
}
