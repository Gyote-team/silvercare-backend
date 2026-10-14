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

    @Transactional
    public ChatSessionResponse open(User actor, CreateChatSessionRequest request) {
        UUID patientId = permissions.resolveTargetPatient(actor, request.patientId());
        ChatSession session = sessions.save(ChatSession.open(patientId, actor.getId(), request.title()));
        return sessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> list(User actor, UUID patientId) {
        UUID target = permissions.resolveTargetPatient(actor, patientId);
        return sessions.findByPatientIdAndStartedByUserIdOrderByCreatedAtDesc(target, actor.getId())
                .stream().map(PrivateChatService::sessionResponse).toList();
    }

    @Transactional
    public List<ChatMessageResponse> ask(User actor, UUID sessionId, AskChatRequest request) {
        ChatSession session = ownSession(actor, sessionId);
        messages.save(ChatMessage.of(sessionId, ChatRole.USER, request.question()));
        AiResultDocumentResponse answer = resultDocuments.create(actor, new CreateAiResultDocumentRequest(
                session.getPatientId(), request.question(), request.questionType(), request.documentIds()
        ));
        String text = answer.content();
        if (answer.jobStatus() == com.gyote.silvercare.global.status.AiJobStatus.FAILED) {
            text = "AI 답변 생성 실패 (AI_GENERATION_FAILED): "
                    + (answer.errorMessage() == null ? "잠시 후 다시 시도해 주세요." : answer.errorMessage());
        } else if (text == null) {
            text = "답변을 만들지 못했어요.";
        }
        messages.save(ChatMessage.of(sessionId, ChatRole.ASSISTANT, text));
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(PrivateChatService::messageResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(User actor, UUID sessionId) {
        ownSession(actor, sessionId);
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(PrivateChatService::messageResponse).toList();
    }

    private ChatSession ownSession(User actor, UUID sessionId) {
        return sessions.findByIdAndStartedByUserId(sessionId, actor.getId())
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED));
    }

    private static ChatSessionResponse sessionResponse(ChatSession session) {
        return new ChatSessionResponse(session.getId(), session.getPatientId(), session.getTitle(), session.getCreatedAt());
    }

    private static ChatMessageResponse messageResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
