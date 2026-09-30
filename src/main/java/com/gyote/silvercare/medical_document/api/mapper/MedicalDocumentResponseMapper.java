package com.gyote.silvercare.medical_document.api.mapper;

import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentDetailResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentListResponseDto;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentPage;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Application 조회 모델을 HTTP 응답 DTO로 변환합니다. 시각은 +09:00 오프셋으로 내보냅니다. */
@Component
public class MedicalDocumentResponseMapper {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public MedicalDocumentDetailResponseDto toResponse(MedicalDocumentView view) {
        return new MedicalDocumentDetailResponseDto(
                view.documentId(),
                view.visitId(),
                view.patientId(),
                view.documentName(),
                view.documentType(),
                view.visitedOn(),
                new MedicalDocumentDetailResponseDto.Author(view.author().name(), view.author().role()),
                view.documentStatus(),
                view.latestAiJobStatus(),
                view.resultStatus(),
                toKst(view.statusChangedAt()),
                view.retryable(),
                view.signedUrl(),
                toKst(view.createdAt())
        );
    }

    public List<MedicalDocumentDetailResponseDto> toResponses(List<MedicalDocumentView> views) {
        return views.stream().map(this::toResponse).toList();
    }

    public MedicalDocumentListResponseDto toPageResponse(MedicalDocumentPage page) {
        return new MedicalDocumentListResponseDto(toResponses(page.items()), page.nextCursor());
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant == null ? null : instant.atOffset(KST);
    }
}
