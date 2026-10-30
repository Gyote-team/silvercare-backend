package com.gyote.silvercare.voice_recording.domain.repository;

import com.gyote.silvercare.voice_recording.domain.entity.VoiceTranscription;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.*;

/** 업로드 중복과 확정 경쟁을 DB에서 제어한다. */
public interface VoiceTranscriptionRepository extends JpaRepository<VoiceTranscription, UUID> {
    /** 같은 사용자의 재요청을 찾는다. */
    Optional<VoiceTranscription> findByUploaderUserIdAndIdempotencyKey(UUID uploader, String key);

    /** 확정·취소·재시도를 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from VoiceTranscription t where t.id=:id")
    Optional<VoiceTranscription> findLockedById(UUID id);

    /** 재접속 후 복구할 최근 작업을 대상과 용도 범위에서 조회한다. */
    List<VoiceTranscription> findTop20ByPatientIdAndPurposeOrderByCreatedAtDesc(
            UUID patientId, String purpose);

    /** 방문에 연결한 최근 녹음을 조회한다. */
    List<VoiceTranscription> findTop20ByVisitIdOrderByCreatedAtDesc(UUID visitId);
}
