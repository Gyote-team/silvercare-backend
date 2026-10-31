package com.gyote.silvercare.ai_chat.api.mapper;

import com.gyote.silvercare.ai_chat.api.dto.response.AiChatMessageResponseDto;
import com.gyote.silvercare.ai_chat.api.dto.response.AiChatSessionResponseDto;
import com.gyote.silvercare.ai_chat.query.model.AiChatMessageView;
import com.gyote.silvercare.ai_chat.query.model.AiChatSessionView;
import org.springframework.stereotype.Component;

import java.util.List;

/** AI 채팅 조회 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class AiChatResponseMapper {

    /** 단일 채팅방 조회 결과를 HTTP 응답으로 변환한다. */
    public AiChatSessionResponseDto toSessionResponse(AiChatSessionView view) {
        return new AiChatSessionResponseDto(view.sessionId(), view.patientId(), view.title(), view.createdAt());
    }

    /** 채팅방 조회 결과 목록을 HTTP 응답 목록으로 변환한다. */
    public List<AiChatSessionResponseDto> toSessionResponses(List<AiChatSessionView> views) {
        return views.stream().map(this::toSessionResponse).toList();
    }

    /** 채팅 메시지 조회 결과 목록을 HTTP 응답 목록으로 변환한다. */
    public List<AiChatMessageResponseDto> toMessageResponses(List<AiChatMessageView> views) {
        return views.stream().map(view -> new AiChatMessageResponseDto(
                view.messageId(), view.role(), view.content(), view.createdAt(), view.resultDocumentId(),
                view.contentFormat(), view.classifiedQuestionType(), view.documentSaveStatus()
        )).toList();
    }
}
