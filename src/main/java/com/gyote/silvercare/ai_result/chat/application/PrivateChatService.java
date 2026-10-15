package com.gyote.silvercare.ai_result.chat.application;

import com.gyote.silvercare.ai_result.api.dto.AiResultDocumentResponse;
import com.gyote.silvercare.ai_result.api.dto.CreateAiResultDocumentRequest;
import com.gyote.silvercare.ai_result.application.AiResultDocumentService;
import com.gyote.silvercare.ai_result.chat.ChatMessage;
import com.gyote.silvercare.ai_result.chat.ChatMessageRepository;
import com.gyote.silvercare.ai_result.chat.ChatRole;
import com.gyote.silvercare.ai_result.chat.ChatSession;
import com.gyote.silvercare.ai_result.chat.ChatSessionRepository;
import com.gyote.silvercare.ai_result.chat.api.AskChatRequest;
import com.gyote.silvercare.ai_result.chat.api.ChatMessageResponse;
import com.gyote.silvercare.ai_result.chat.api.ChatSessionResponse;
import com.gyote.silvercare.ai_result.chat.api.CreateChatSessionRequest;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivateChatService {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final CareRelationPermissionService permissions;
    private final AiResultDocumentService resultDocuments;

    // 접근 가능한 질문 대상을 기준으로 새 채팅방을 생성한다.
    @Transactional
    public ChatSessionResponse open(User actor, CreateChatSessionRequest request) {
        UUID patientId = permissions.resolveTargetPatient(actor, request.patientId());
        ChatSession session = sessions.save(ChatSession.open(patientId, actor.getId(), request.title()));
        return sessionResponse(session);
    }

    // 질문 대상과 현재 사용자의 채팅방 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> list(User actor, UUID patientId) {
        UUID target = permissions.resolveTargetPatient(actor, patientId);
        return sessions.findByPatientIdAndStartedByUserIdOrderByCreatedAtDesc(target, actor.getId())
                .stream().map(PrivateChatService::sessionResponse).toList();
    }

    // 사용자 질문과 AI 답변을 저장하고 전체 대화 내역을 반환한다.
    @Transactional
    public List<ChatMessageResponse> ask(User actor, UUID sessionId, AskChatRequest request) {
        ChatSession session = ownSession(actor, sessionId);
        if ("새 채팅".equals(session.getTitle())) {
            session.rename(questionTitle(request.question()));
        }
        messages.save(ChatMessage.of(sessionId, ChatRole.USER, request.question()));
        AiResultDocumentResponse answer = resultDocuments.create(actor, new CreateAiResultDocumentRequest(
                session.getPatientId(), request.question(), request.questionType(), request.documentIds()
        ));
        String text = answer.content();
        if (answer.jobStatus() == com.gyote.silvercare.global.status.AiJobStatus.FAILED) {
            text = "AI 답변 생성 실패 (AI_GENERATION_FAILED): "
                    + (answer.errorMessage() == null ? "잠시 후 다시 시도해 주세요." : answer.errorMessage());
        } else if (text == null || text.isBlank()) {
            text = "선택한 문서에서 답변할 수 있는 내용을 찾지 못했어요. 분석이 완료된 다른 문서를 선택해 주세요.";
        }
        messages.save(ChatMessage.answer(sessionId, text, answer.resultDocumentId()));
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(PrivateChatService::messageResponse).toList();
    }

    // 채팅방 소유권을 확인하고 전체 메시지를 시간순으로 조회한다.
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(User actor, UUID sessionId) {
        ownSession(actor, sessionId);
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(PrivateChatService::messageResponse).toList();
    }

    // 현재 사용자가 생성한 채팅방인지 확인한다.
    private ChatSession ownSession(User actor, UUID sessionId) {
        return sessions.findByIdAndStartedByUserId(sessionId, actor.getId())
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED));
    }

    // 채팅방 엔티티를 목록 응답 형태로 변환한다.
    private static ChatSessionResponse sessionResponse(ChatSession session) {
        return new ChatSessionResponse(session.getId(), session.getPatientId(), session.getTitle(), session.getCreatedAt());
    }

    // 채팅 메시지 엔티티를 대화 응답 형태로 변환한다.
    private static ChatMessageResponse messageResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt(), message.getResultDocumentId());
    }

    // 첫 질문을 공백 정리와 길이 제한을 적용한 채팅방 제목으로 만든다.
    private static String questionTitle(String question) {
        String normalized = question == null ? "" : question.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "새 채팅";
        }
        return normalized.length() > 38 ? normalized.substring(0, 37).stripTrailing() + "…" : normalized;
    }
}
