package com.gyote.silvercare.ai_chat.command.application;

import com.gyote.silvercare.ai_chat.domain.ChatRole;
import com.gyote.silvercare.ai_chat.domain.entity.ChatMessage;
import com.gyote.silvercare.ai_chat.domain.entity.ChatSession;
import com.gyote.silvercare.ai_chat.domain.repository.ChatMessageRepository;
import com.gyote.silvercare.ai_chat.domain.repository.ChatSessionRepository;
import com.gyote.silvercare.ai_chat.error.AiChatErrorCode;
import com.gyote.silvercare.ai_chat.query.model.AiChatMessageView;
import com.gyote.silvercare.ai_chat.query.model.AiChatSessionView;
import com.gyote.silvercare.ai_result.command.application.AiResultDocumentCommandService;
import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.gyote.silvercare.ai_result.query.model.AiResultDocumentView;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** AI 채팅방 생성과 문서 근거 기반 질문 처리를 담당한다. */
@Service
@RequiredArgsConstructor
public class AiChatCommandService {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final CareRelationPermissionService permissions;
    private final AiResultDocumentCommandService resultDocuments;

    /** 접근 가능한 질문 대상을 기준으로 새 채팅방을 생성한다. */
    @Transactional
    public AiChatSessionView createSession(User actor, UUID requestedPatientId, String title) {
        UUID patientId = permissions.resolveTargetPatient(actor, requestedPatientId);
        ChatSession session = sessions.save(ChatSession.open(patientId, actor.getId(), normalizedTitle(title)));
        return AiChatSessionView.from(session);
    }

    /** 사용자 질문과 AI 답변을 저장하고 전체 대화 내역을 반환한다. */
    @Transactional
    public List<AiChatMessageView> ask(
            User actor,
            UUID sessionId,
            String question,
            QuestionType questionType,
            List<UUID> documentIds
    ) {
        ChatSession session = requireOwnedSession(actor, sessionId);
        if ("새 채팅".equals(session.getTitle())) {
            session.rename(questionTitle(question));
        }
        messages.save(ChatMessage.of(sessionId, ChatRole.USER, question));
        AiResultDocumentView answer = resultDocuments.create(
                actor, session.getPatientId(), question, questionType, documentIds
        );
        messages.save(ChatMessage.answer(sessionId, answerText(answer), answer.resultDocumentId()));
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(AiChatMessageView::from).toList();
    }

    /** 현재 사용자가 생성한 채팅방인지 확인한다. */
    private ChatSession requireOwnedSession(User actor, UUID sessionId) {
        return sessions.findByIdAndStartedByUserId(sessionId, actor.getId())
                .orElseThrow(() -> new BusinessException(AiChatErrorCode.CHAT_ACCESS_DENIED));
    }

    /** AI 생성 상태와 오류 사유를 사용자가 이해할 수 있는 답변으로 변환한다. */
    private static String answerText(AiResultDocumentView answer) {
        if (answer.jobStatus() == AiJobStatus.FAILED) {
            String reason = answer.errorMessage() == null ? "잠시 후 다시 시도해 주세요." : answer.errorMessage();
            return "AI 답변 생성 실패 (AI_GENERATION_FAILED): " + reason;
        }
        if (answer.content() == null || answer.content().isBlank()) {
            return "선택한 문서에서 답변할 수 있는 내용을 찾지 못했어요. 분석이 완료된 다른 문서를 선택해 주세요.";
        }
        return answer.content();
    }

    /** 비어 있는 요청 제목을 새 채팅 기본 제목으로 정규화한다. */
    private static String normalizedTitle(String title) {
        return title == null || title.isBlank() ? "새 채팅" : title.trim();
    }

    /** 첫 질문을 공백 정리와 길이 제한을 적용한 채팅방 제목으로 만든다. */
    private static String questionTitle(String question) {
        String normalized = question == null ? "" : question.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "새 채팅";
        }
        return normalized.length() > 38 ? normalized.substring(0, 37).stripTrailing() + "…" : normalized;
    }
}
