package com.gyote.silvercare.care_relation.domain.repository;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareRelationRepository extends JpaRepository<CareRelation, UUID> {

    /** 환자 기준 돌봄 관계를 요청일 내림차순으로 조회한다. */
    List<CareRelation> findByPatientIdOrderByRequestedAtDesc(UUID patientId);

    /** 보호자 기준 돌봄 관계를 요청일 내림차순으로 조회한다. */
    List<CareRelation> findByCaregiverIdOrderByRequestedAtDesc(UUID caregiverId);

    List<CareRelation> findByPatientIdAndStatus(UUID patientId, CareRelationStatus status);

    List<CareRelation> findByCaregiverIdAndStatus(UUID caregiverId, CareRelationStatus status);

    /** 환자와 보호자 및 허용 상태에 해당하는 첫 관계를 조회한다. */
    Optional<CareRelation> findFirstByPatientIdAndCaregiverIdAndStatusIn(
            UUID patientId,
            UUID caregiverId,
            List<CareRelationStatus> statuses
    );

    /** 특정 환자와 보호자 사이에 지정된 상태의 관계가 존재하는지 확인한다. */
    boolean existsByPatientIdAndCaregiverIdAndStatus(
            UUID patientId,
            UUID caregiverId,
            CareRelationStatus status
    );

    Optional<CareRelation> findFirstByPatientIdAndCaregiverIdAndStatus(
            UUID patientId,
            UUID caregiverId,
            CareRelationStatus status
    );

    /** 사용자 행을 잠그기 전에 관계 엔티티를 영속성 컨텍스트에 올리지 않고 당사자만 조회한다. */
    @Query("select r.patientId as patientId, r.caregiverId as caregiverId from CareRelation r where r.id = :id")
    Optional<Participants> findParticipantsById(@Param("id") UUID id);

    /** 관계 당사자 식별자 프로젝션이다. */
    interface Participants {
        UUID getPatientId();

        UUID getCaregiverId();
    }
}
