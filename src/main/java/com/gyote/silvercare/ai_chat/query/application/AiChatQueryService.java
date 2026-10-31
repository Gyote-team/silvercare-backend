package com.gyote.silvercare.ai_chat.query.application;

import com.gyote.silvercare.ai_chat.domain.entity.ChatSession;
import com.gyote.silvercare.ai_chat.domain.repository.ChatMessageRepository;
import com.gyote.silvercare.ai_chat.domain.repository.ChatSessionRepository;
import com.gyote.silvercare.ai_chat.error.AiChatErrorCode;
import com.gyote.silvercare.ai_chat.query.model.AiChatMessageView;
import com.gyote.silvercare.ai_chat.query.model.AiChatSessionView;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.ai_result.domain.repository.AiResultDocumentRepository;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 현재 사용자의 AI 채팅방과 메시지를 읽기 전용으로 조회한다. */
@Service
@RequiredArgsConstructor
public class AiChatQueryService {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final CareRelationPermissionService permissions;
    private final AiResultDocumentRepository resultDocuments;

    /** 질문 대상과 현재 사용자의 채팅방 목록을 최근 생성 순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<AiChatSessionView> findSessions(User actor, UUID requestedPatientId) {
        UUID patientId = permissions.resolveTargetPatient(actor, requestedPatientId);
        return sessions.findByPatientIdAndStartedByUserIdOrderByCreatedAtDesc(patientId, actor.getId())
                .stream().map(AiChatSessionView::from).toList();
    }

    /** 채팅방 소유권과 현재 환자 열람 권한을 확인하고 전체 메시지를 시간순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<AiChatMessageView> findMessages(User actor, UUID sessionId) {
        ChatSession session = requireOwnedSession(actor, sessionId);
        permissions.requireDocumentAccess(actor, session.getPatientId());
        var storedMessages = messages.findBySessionIdOrderByCreatedAtAsc(sessionId);
        var resultIds = storedMessages.stream().map(message -> message.getResultDocumentId())
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, com.gyote.silvercare.ai_result.domain.entity.AiResultDocument> resultById =
                resultDocuments.findAllById(resultIds).stream().collect(Collectors.toMap(
                        com.gyote.silvercare.ai_result.domain.entity.AiResultDocument::getId, Function.identity()));
        return storedMessages.stream()
                .map(message -> AiChatMessageView.from(message, resultById.get(message.getResultDocumentId())))
                .toList();
    }

    /** 현재 사용자가 생성한 채팅방인지 확인한다. */
    private ChatSession requireOwnedSession(User actor, UUID sessionId) {
        return sessions.findByIdAndStartedByUserId(sessionId, actor.getId())
                .orElseThrow(() -> new BusinessException(AiChatErrorCode.CHAT_ACCESS_DENIED));
    }
}
