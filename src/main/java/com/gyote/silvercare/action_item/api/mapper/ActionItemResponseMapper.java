package com.gyote.silvercare.action_item.api.mapper;

import com.gyote.silvercare.action_item.api.dto.response.ActionItemPageResponseDto;
import com.gyote.silvercare.action_item.api.dto.response.ActionItemResponseDto;
import com.gyote.silvercare.action_item.query.model.ActionItemPageView;
import com.gyote.silvercare.action_item.query.model.ActionItemRow;

import org.springframework.stereotype.Component;

/** API에 의존하지 않는 조회 결과를 후보 응답 계약으로 변환한다. */
@Component
public class ActionItemResponseMapper {
    /** 목록과 다음 페이지 커서를 HTTP 응답 모델로 변환한다. */
    public ActionItemPageResponseDto toResponse(ActionItemPageView page) {
        return new ActionItemPageResponseDto(
                page.items().stream().map(this::toResponse).toList(), page.nextCursor());
    }

    /** 검증된 근거만 응답에 포함하고 기존 발췌문 필드에도 같은 값을 제공한다. */
    public ActionItemResponseDto toResponse(ActionItemRow row) {
        ActionItemResponseDto.Source source =
                row.source() == null
                        ? null
                        : new ActionItemResponseDto.Source(
                                row.source().pageNo(), row.source().sourceText());
        return new ActionItemResponseDto(
                row.actionItemId(),
                row.patientId(),
                row.visitId(),
                row.documentId(),
                row.documentName(),
                row.type(),
                row.title(),
                row.description(),
                row.estimatedAt(),
                row.approvalStatus(),
                row.cancelReason(),
                row.sourceDocumentDeleted(),
                row.sourceItemId(),
                row.confidence(),
                source,
                source == null ? null : source.sourceText(),
                row.approvedByUserId(),
                row.approvedAt(),
                row.createdAt());
    }
}
