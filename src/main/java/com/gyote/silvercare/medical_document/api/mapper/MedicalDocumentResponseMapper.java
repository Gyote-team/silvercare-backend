package com.gyote.silvercare.medical_document.api.mapper;

import com.gyote.silvercare.medical_document.api.dto.response.DocumentUploadResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentDetailResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentListResponseDto;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadResult;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentPage;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 조회 모델과 업로드 결과를 HTTP 응답 DTO로 변환합니다. 시각은 +09:00 오프셋으로 내보냅니다. */
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

    /** 업로드 결과를 업로드 응답 DTO로 변환해 반환합니다. storageKey는 담지 않습니다. */
    public DocumentUploadResponseDto toUploadResponse(DocumentUploadResult result) {
        MedicalDocument document = result.document();
        return new DocumentUploadResponseDto(
                document.getId(),
                document.getVisitId(),
                document.getMimeType(),
                document.getStatus(),
                result.latestAiJobStatus(),
                document.getRequestId()
        );
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant == null ? null : instant.atOffset(KST);
    }
}
