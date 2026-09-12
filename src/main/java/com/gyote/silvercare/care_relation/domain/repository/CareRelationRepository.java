package com.gyote.silvercare.care_relation.domain.repository;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareRelationRepository extends JpaRepository<CareRelation, UUID> {

    List<CareRelation> findByPatientIdOrderByRequestedAtDesc(UUID patientId);

    List<CareRelation> findByCaregiverIdOrderByRequestedAtDesc(UUID caregiverId);

    Optional<CareRelation> findFirstByPatientIdAndCaregiverIdAndStatusIn(
            UUID patientId,
            UUID caregiverId,
            List<CareRelationStatus> statuses
    );
}
