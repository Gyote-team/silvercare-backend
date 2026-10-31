package com.gyote.silvercare.ai_chat.api.controller;

import com.gyote.silvercare.ai_chat.api.dto.request.AskChatRequestDto;
import com.gyote.silvercare.ai_chat.api.dto.request.CreateChatSessionRequestDto;
import com.gyote.silvercare.ai_chat.api.dto.response.AiChatMessageResponseDto;
import com.gyote.silvercare.ai_chat.api.dto.response.AiChatSessionResponseDto;
import com.gyote.silvercare.ai_chat.api.mapper.AiChatResponseMapper;
import com.gyote.silvercare.ai_chat.command.application.AiChatCommandService;
import com.gyote.silvercare.ai_chat.query.application.AiChatQueryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import com.gyote.silvercare.ai_result.domain.repository.AiDetailedDocumentRepository;
import com.gyote.silvercare.ai_result.domain.entity.AiDetailedDocument;

/** AI 채팅방 생성, 목록, 질문, 메시지 조회 API를 제공한다. */
@RestController
@RequestMapping("/api/ai-chat-sessions")
@RequiredArgsConstructor
public class AiChatApiController {

    private final AiChatCommandService commands;
    private final AiChatQueryService queries;
    private final AiChatResponseMapper mapper;
    private final UserQueryService users;
    private final AiDetailedDocumentRepository detailedDocuments;

    /** 선택한 질문 대상과 대화할 새 채팅방을 생성한다. */
    @PostMapping
    public AiChatSessionResponseDto createSession(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody CreateChatSessionRequestDto request
    ) {
        return mapper.toSessionResponse(
                commands.createSession(actor(principal), request.patientId(), request.title())
        );
    }

    /** 질문 대상과 현재 사용자의 채팅방 목록을 조회한다. */
    @GetMapping
    public List<AiChatSessionResponseDto> findSessions(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId
    ) {
        return mapper.toSessionResponses(queries.findSessions(actor(principal), patientId));
    }

    /** 채팅방에 질문을 저장하고 문서 근거 기반 AI 답변을 생성한다. */
    @PostMapping("/{sessionId}/messages")
    public List<AiChatMessageResponseDto> ask(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AskChatRequestDto request
    ) {
        return mapper.toMessageResponses(commands.ask(
                actor(principal), sessionId, request.question(), request.questionType(), request.documentIds()
        ));
    }

    /** 채팅방의 전체 대화 내역을 조회한다. */
    @GetMapping("/{sessionId}/messages")
    public List<AiChatMessageResponseDto> findMessages(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID sessionId
    ) {
        return mapper.toMessageResponses(queries.findMessages(actor(principal), sessionId));
    }

    /** 현재 채팅방에서 생성된 상세 AI 문서만 최신순으로 보여준다. */
    @GetMapping("/{sessionId}/documents")
    public List<ChatDocumentResponse> findDocuments(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID sessionId) {
        User actor = actor(principal);
        return queries.findMessages(actor, sessionId).stream().map(message -> message.resultDocumentId())
                .filter(java.util.Objects::nonNull).map(detailedDocuments::findByResultDocumentId)
                .flatMap(Optional::stream).filter(d -> d.getRequestedByUserId().equals(actor.getId()))
                .sorted(java.util.Comparator.comparing(AiDetailedDocument::getCreatedAt).reversed())
                .map(ChatDocumentResponse::of).toList();
    }

    public record ChatDocumentResponse(UUID detailedDocumentId, String status, String title, String pdfFileName, java.time.Instant createdAt) {
        static ChatDocumentResponse of(AiDetailedDocument d) { return new ChatDocumentResponse(d.getId(), d.getStatus().name(), d.getTitle(), d.getPdfFileName(), d.getCreatedAt()); }
    }

    /** 인증 정보에서 현재 요청 사용자를 안전하게 조회한다. */
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
