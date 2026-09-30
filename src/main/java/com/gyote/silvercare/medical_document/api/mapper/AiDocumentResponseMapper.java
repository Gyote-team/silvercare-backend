package com.gyote.silvercare.medical_document.api.mapper;

import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentDetailResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentExplanationStatusResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentListItemResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentListResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentSectionItemResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentSectionResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentSectionsResponseDto;
import com.gyote.silvercare.medical_document.query.model.AiDocumentDetailView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentExplanationStatusView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionsView;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AiDocumentResponseMapper {

    /** 목록 조회 모델을 공통 응답 계약의 목록 DTO로 변환한다. */
    public AiDocumentListResponseDto toListResponse(Page<AiDocumentListView> page) {
        List<AiDocumentListItemResponseDto> items = page.getContent().stream()
                .map(this::toListItem)
                .toList();
        return new AiDocumentListResponseDto(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    /** 상세 조회 모델을 공통 응답 계약의 상세 DTO로 변환한다. */
    public AiDocumentDetailResponseDto toDetailResponse(AiDocumentDetailView view) {
        return new AiDocumentDetailResponseDto(
                uuid(view.documentId()),
                view.documentName(),
                view.documentType().name(),
                uuid(view.visitId()),
                view.documentStatus().name(),
                view.jobStatus().name(),
                view.resultStatus() == null ? null : view.resultStatus().name(),
                view.title(),
                view.content(),
                view.createdAt(),
                view.sectionCount(),
                view.citationCount()
        );
    }

    /** 섹션 조회 모델을 공통 응답 계약의 섹션 DTO로 변환한다. */
    public AiDocumentSectionsResponseDto toSectionsResponse(AiDocumentSectionsView view) {
        List<AiDocumentSectionResponseDto> sections = view.sections().stream()
                .map(this::toSection)
                .toList();
        return new AiDocumentSectionsResponseDto(uuid(view.documentId()), sections);
    }

    /** 상태 조회 모델을 공통 응답 계약의 상태 DTO로 변환한다. */
    public AiDocumentExplanationStatusResponseDto toStatusResponse(AiDocumentExplanationStatusView view) {
        return new AiDocumentExplanationStatusResponseDto(
                uuid(view.documentId()),
                view.jobStatus().name(),
                view.resultStatus() == null ? null : view.resultStatus().name(),
                view.currentStep(),
                view.progress(),
                view.completedAt(),
                view.detailUrl(),
                view.failedStep(),
                view.errorCode(),
                view.retryable(),
                view.originalDocumentUrl()
        );
    }

    /** 목록 조회 모델 한 건을 목록 항목 응답 DTO로 변환한다. */
    private AiDocumentListItemResponseDto toListItem(AiDocumentListView view) {
        return new AiDocumentListItemResponseDto(
                uuid(view.documentId()),
                view.documentName(),
                view.documentType().name(),
                uuid(view.visitId()),
                view.visitedOn() == null ? null : view.visitedOn().toString(),
                view.createdAt(),
                view.authorName(),
                view.documentStatus().name(),
                view.jobStatus().name(),
                view.resultStatus() == null ? null : view.resultStatus().name()
        );
    }

    /** 섹션 조회 모델을 섹션 응답 DTO로 변환한다. */
    private AiDocumentSectionResponseDto toSection(AiDocumentSectionView view) {
        List<AiDocumentSectionItemResponseDto> items = view.items().stream()
                .map(item -> new AiDocumentSectionItemResponseDto(
                        uuid(item.sentenceId()),
                        uuid(item.sourceItemId()),
                        item.label(),
                        item.value(),
                        item.unit(),
                        item.hasSource(),
                        uuid(item.citationId())
                ))
                .toList();
        return new AiDocumentSectionResponseDto(
                uuid(view.sectionId()),
                view.sectionType().name(),
                view.title(),
                items
        );
    }

    /** UUID를 API 응답용 문자열로 변환한다. */
    private static String uuid(UUID value) {
        return value == null ? null : value.toString();
    }
}
