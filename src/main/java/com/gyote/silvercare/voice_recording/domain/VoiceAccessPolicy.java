package com.gyote.silvercare.voice_recording.domain;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;
import com.gyote.silvercare.voice_recording.domain.entity.VoiceTranscription;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** 음성 원본·초안도 최종 기록과 동일한 ACTIVE 연결 범위로 제한한다. */
@Component
public class VoiceAccessPolicy {
    private final HealthRecordAccessPolicy access;
    private final VisitRepository visits;

    public VoiceAccessPolicy(HealthRecordAccessPolicy access, VisitRepository visits) {
        this.access = access;
        this.visits = visits;
    }

    /** 개인의 대상 생략과 보호자의 대상 필수 조건을 검증한다. */
    public UUID patient(User actor, UUID requested) {
        return access.requirePatient(actor, requested);
    }

    /** 기존 음성 작업의 현재 접근권한을 검사한다. */
    public void require(User actor, VoiceTranscription row) {
        patient(actor, row.getPatientId());
    }

    /** 녹음의 방문이 대상 개인에게 속하며 취소되지 않았는지 검사한다. */
    public void writableVisit(UUID patient, UUID visit) {
        if (visit == null) return;
        var row =
                visits.findByIdAndDeletedAtIsNull(visit)
                        .orElseThrow(() -> new BusinessException(GlobalErrorCode.INVALID_REQUEST));
        if (!row.getPatientId().equals(patient) || row.getStatus() == VisitStatus.CANCELED)
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
    }

    /** 방문 녹음 목록은 취소 방문에서도 소유 범위를 유지한다. */
    public UUID visitPatient(User actor, UUID visit) {
        var row =
                visits.findByIdAndDeletedAtIsNull(visit)
                        .orElseThrow(() -> new BusinessException(GlobalErrorCode.INVALID_REQUEST));
        return patient(actor, row.getPatientId());
    }
}
