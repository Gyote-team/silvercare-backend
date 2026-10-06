package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentCitationsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentFactsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentPageResponseDto;
import com.gyote.silvercare.medical_document.query.application.AiDocumentEvidenceQueryService;
import com.gyote.silvercare.medical_document.query.application.AiDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 원문 근거 컨트롤러의 인증 사용자·경로 식별자 위임을 검증한다. */
class AiDocumentEvidenceControllerTest {

    private final AiDocumentEvidenceQueryService evidence = mock(AiDocumentEvidenceQueryService.class);
    private final AiDocumentQueryService queries = mock(AiDocumentQueryService.class);
    private final User actor = new User();
    private final UUID userId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private AiDocumentEvidenceController controller;
    private OAuth2User principal;

    @BeforeEach
    void setUp() {
        controller = new AiDocumentEvidenceController(evidence, queries);
        principal = mock(OAuth2User.class);
        when(principal.getAttributes()).thenReturn(Map.of("userId", userId.toString()));
        when(queries.requireUser(userId)).thenReturn(actor);
    }

    @Test
    void factsResolvesAuthenticatedUserAndDelegatesParsedDocumentId() {
        AiDocumentFactsResponseDto expected = new AiDocumentFactsResponseDto(documentId.toString(), List.of());
        when(evidence.facts(actor, documentId)).thenReturn(expected);

        assertThat(controller.facts(principal, documentId.toString())).isSameAs(expected);

        verify(queries).requireUser(userId);
        verify(evidence).facts(actor, documentId);
    }

    @Test
    void citationsAndPageDelegateToEvidenceService() {
        AiDocumentCitationsResponseDto citations = new AiDocumentCitationsResponseDto(documentId.toString(), List.of());
        AiDocumentPageResponseDto page = new AiDocumentPageResponseDto(
                documentId.toString(), 2, "https://storage.example/page", Instant.parse("2026-10-06T00:05:00Z"),
                true, "anchor-1", null, 1200, 1800
        );
        when(evidence.citations(actor, documentId, null)).thenReturn(citations);
        when(evidence.page(actor, documentId, 2, "anchor-1")).thenReturn(page);

        assertThat(controller.citations(principal, documentId.toString(), null)).isSameAs(citations);
        assertThat(controller.page(principal, documentId.toString(), 2, "anchor-1")).isSameAs(page);
    }

    @Test
    void invalidDocumentIdIsRejectedBeforeServiceDelegation() {
        assertThatThrownBy(() -> controller.facts(principal, "not-a-uuid"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("조회 조건");
    }

    @Test
    void missingAuthenticatedUserIdIsRejected() {
        OAuth2User anonymous = mock(OAuth2User.class);
        when(anonymous.getAttributes()).thenReturn(Map.of());

        assertThatThrownBy(() -> controller.facts(anonymous, documentId.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자");
    }
}
