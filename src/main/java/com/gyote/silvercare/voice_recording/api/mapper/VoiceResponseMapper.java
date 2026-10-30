package com.gyote.silvercare.voice_recording.api.mapper;

import com.gyote.silvercare.voice_recording.api.dto.response.VoiceTranscriptionResponseDto;
import com.gyote.silvercare.voice_recording.query.model.VoiceTranscriptionView;

import org.springframework.stereotype.Component;

/** 내부 조회 모델을 음성 HTTP 계약으로 변환한다. */
@Component
public class VoiceResponseMapper {
    /** 원본 저장 위치 없이 초안과 현재 버전을 변환한다. */
    public VoiceTranscriptionResponseDto toResponse(VoiceTranscriptionView v) {
        return new VoiceTranscriptionResponseDto(
                v.transcriptionId(),
                v.patientId(),
                v.visitId(),
                v.purpose(),
                v.status(),
                "/api/transcriptions/" + v.transcriptionId(),
                v.draftText(),
                v.errorCode(),
                v.recordId(),
                v.confirmedText(),
                v.version(),
                v.createdAt());
    }
}
