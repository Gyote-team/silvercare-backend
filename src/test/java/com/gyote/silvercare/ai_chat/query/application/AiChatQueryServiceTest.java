package com.gyote.silvercare.ai_chat.query.application;

import com.gyote.silvercare.ai_chat.domain.ChatRole;
import com.gyote.silvercare.ai_chat.domain.entity.ChatMessage;
import com.gyote.silvercare.ai_chat.domain.entity.ChatSession;
import com.gyote.silvercare.ai_chat.domain.repository.ChatMessageRepository;
import com.gyote.silvercare.ai_chat.domain.repository.ChatSessionRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiChatQueryServiceTest {
    private final ChatSessionRepository sessions = mock(ChatSessionRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final CareRelationPermissionService permissions = mock(CareRelationPermissionService.class);
    private final AiChatQueryService service = new AiChatQueryService(sessions, messages, permissions);

    @Test
    void revokedCareRelationCannotReadStoredMessages() {
        User actor = new User();
        actor.setId(UUID.randomUUID());
        UUID patientId = UUID.randomUUID();
        ChatSession session = ChatSession.open(patientId, actor.getId(), "채팅방");
        when(sessions.findByIdAndStartedByUserId(session.getId(), actor.getId()))
                .thenReturn(Optional.of(session));
        doThrow(new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED))
                .when(permissions).requireDocumentAccess(actor, patientId);

        assertThatThrownBy(() -> service.findMessages(actor, session.getId()))
                .isInstanceOf(BusinessException.class);

        verify(permissions).requireDocumentAccess(actor, patientId);
        verifyNoInteractions(messages);
    }

    @Test
    void currentPatientAccessAllowsStoredMessages() {
        User actor = new User();
        actor.setId(UUID.randomUUID());
        UUID patientId = UUID.randomUUID();
        ChatSession session = ChatSession.open(patientId, actor.getId(), "채팅방");
        ChatMessage answer = ChatMessage.answer(session.getId(), "기존 답변", UUID.randomUUID());
        when(sessions.findByIdAndStartedByUserId(session.getId(), actor.getId()))
                .thenReturn(Optional.of(session));
        when(messages.findBySessionIdOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(answer));

        var result = service.findMessages(actor, session.getId());

        verify(permissions).requireDocumentAccess(actor, patientId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo(ChatRole.ASSISTANT);
    }
}
