package com.gyote.silvercare.ai_result.api.mapper;

import com.gyote.silvercare.ai_result.api.dto.response.AiResultDocumentResponseDto;
import com.gyote.silvercare.ai_result.query.model.AiResultDocumentView;
import org.springframework.stereotype.Component;

/** AI 답변 문서 조회 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class AiResultDocumentResponseMapper {

    /** 원문 근거 표시에 필요한 필드만 HTTP 응답으로 변환한다. */
    public AiResultDocumentResponseDto toResponse(AiResultDocumentView view) {
        return new AiResultDocumentResponseDto(
                view.resultDocumentId(), view.citationJson(), view.errorMessage()
        );
    }
}
