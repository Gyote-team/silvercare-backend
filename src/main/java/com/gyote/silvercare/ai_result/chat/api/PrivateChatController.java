package com.gyote.silvercare.ai_result.chat.api;

import com.gyote.silvercare.ai_result.chat.application.PrivateChatService;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.error.UserErrorCode;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PrivateChatController {

    private final PrivateChatService chats;
    private final UserQueryService users;

    // 선택한 질문 대상과 대화할 새 채팅방을 생성한다.
    @PostMapping("/api/ai-chat-sessions")
    public ChatSessionResponse open(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody CreateChatSessionRequest request
    ) {
        return chats.open(actor(principal), request);
    }

    // 질문 대상과 현재 사용자의 채팅방 목록을 조회한다.
    @GetMapping("/api/ai-chat-sessions")
    public List<ChatSessionResponse> list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId
    ) {
        return chats.list(actor(principal), patientId);
    }

    // 채팅방에 질문을 저장하고 문서 근거 기반 AI 답변을 생성한다.
    @PostMapping("/api/ai-chat-sessions/{sessionId}/messages")
    public List<ChatMessageResponse> ask(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AskChatRequest request
    ) {
        return chats.ask(actor(principal), sessionId, request);
    }

    // 채팅방의 전체 대화 내역을 조회한다.
    @GetMapping("/api/ai-chat-sessions/{sessionId}/messages")
    public List<ChatMessageResponse> messages(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID sessionId
    ) {
        return chats.messages(actor(principal), sessionId);
    }

    // 인증 정보에서 현재 요청 사용자를 안전하게 조회한다.
    private User actor(OAuth2User principal) {
        Object userId = principal.getAttributes().get("userId");
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        try {
            return users.requireById(UUID.fromString(String.valueOf(userId)));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
