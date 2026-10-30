package com.gyote.silvercare.voice_recording.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.voice_recording.domain.VoiceAccessPolicy;
import com.gyote.silvercare.voice_recording.domain.entity.VoiceTranscription;
import com.gyote.silvercare.voice_recording.domain.repository.VoiceTranscriptionRepository;
import com.gyote.silvercare.voice_recording.error.VoiceRecordingErrorCode;
import com.gyote.silvercare.voice_recording.query.model.VoiceTranscriptionView;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 현재 연결 관계를 매번 확인하며 원본·대상 변경 없이 초안을 조회한다. */
@Service
@Transactional(readOnly = true)
public class VoiceQueryService {
    private final VoiceTranscriptionRepository jobs;
    private final VoiceAccessPolicy access;

    public VoiceQueryService(VoiceTranscriptionRepository jobs, VoiceAccessPolicy access) {
        this.jobs = jobs;
        this.access = access;
    }

    /** 접근 가능한 초안과 확정 참조를 반환한다. */
    public VoiceTranscriptionView detail(User actor, UUID id) {
        var row =
                jobs.findById(id)
                        .orElseThrow(
                                () -> new BusinessException(VoiceRecordingErrorCode.NOT_FOUND));
        access.require(actor, row);
        return view(row);
    }

    /** 건강 음성 또는 특정 방문 녹음의 최근 작업을 복구한다. */
    public List<VoiceTranscriptionView> list(User actor, UUID patientId, UUID visitId) {
        if (visitId != null) {
            access.visitPatient(actor, visitId);
            return jobs.findTop20ByVisitIdOrderByCreatedAtDesc(visitId).stream()
                    .map(this::view)
                    .toList();
        }
        UUID target = access.patient(actor, patientId);
        return jobs
                .findTop20ByPatientIdAndPurposeOrderByCreatedAtDesc(target, "HEALTH_RECORD")
                .stream()
                .map(this::view)
                .toList();
    }

    private VoiceTranscriptionView view(VoiceTranscription row) {
        return new VoiceTranscriptionView(
                row.getId(),
                row.getPatientId(),
                row.getVisitId(),
                row.getPurpose(),
                row.getStatus(),
                row.getDraftText(),
                row.getErrorCode(),
                row.getRecordId(),
                row.getConfirmedText(),
                row.getVersion(),
                row.getCreatedAt());
    }
}
