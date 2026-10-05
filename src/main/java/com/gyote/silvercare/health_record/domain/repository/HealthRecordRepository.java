package com.gyote.silvercare.health_record.domain.repository;

import com.gyote.silvercare.health_record.domain.HealthRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    Optional<HealthRecord> findByPatientIdAndAuthorUserIdAndIdempotencyKey(
            UUID patientId, UUID authorUserId, String idempotencyKey);

    List<HealthRecord> findByPatientIdAndDeletedAtIsNullOrderByRecordedAtDescIdDesc(UUID patientId, Pageable pageable);
}
