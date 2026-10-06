package com.gyote.silvercare.medical_document.query.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentCitationsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentFactsResponseDto;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentEvidenceRepository;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentRepository;
import com.gyote.silvercare.medical_document.query.model.AiDocumentCitationRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentFactRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentPageRow;
import com.gyote.silvercare.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AI 설명의 원문값 검증·인용·페이지 URL 발급 규칙을 검증한다. */
class AiDocumentEvidenceQueryServiceTest {

    private final AiDocumentRepository documents = mock(AiDocumentRepository.class);
    private final AiDocumentEvidenceRepository evidence = mock(AiDocumentEvidenceRepository.class);
    private final CareRelationPermissionService permissions = mock(CareRelationPermissionService.class);
    private final DocumentStoragePort storage = mock(DocumentStoragePort.class);

    private final UUID documentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final User actor = new User();

    private AiDocumentEvidenceQueryService service;

    @BeforeEach
    void setUp() {
        MedicalDocument document = mock(MedicalDocument.class);
        when(document.getPatientId()).thenReturn(patientId);
        when(document.isDeleted()).thenReturn(false);
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        service = new AiDocumentEvidenceQueryService(
                documents, evidence, permissions, storage, new ObjectMapper()
        );
    }

    @Test
    void factsHideOnlyMismatchedDisplayValueAndKeepOriginalEvidence() {
        AiDocumentFactRow matched = fact("fact-1", "LAB_VALUE", " 6.5 ", "6.5", "%", 1);
        AiDocumentFactRow mismatched = fact("fact-2", "MEDICATION", "1정", "2정", null, 2);
        when(evidence.findFacts(documentId)).thenReturn(List.of(matched, mismatched));

        AiDocumentFactsResponseDto result = service.facts(actor, documentId);

        assertThat(result.documentId()).isEqualTo(documentId.toString());
        assertThat(result.items()).extracting(AiDocumentFactsResponseDto.FactItem::validationStatus)
                .containsExactly("MATCHED", "MISMATCHED");
        assertThat(result.items().get(0))
                .extracting(AiDocumentFactsResponseDto.FactItem::factType,
                        AiDocumentFactsResponseDto.FactItem::displayValue,
                        AiDocumentFactsResponseDto.FactItem::originalValue)
                .containsExactly("TEST_VALUE", " 6.5 ", "6.5");
        assertThat(result.items().get(1).displayValue()).isNull();
        assertThat(result.items().get(1).originalValue()).isEqualTo("2정");
        verify(permissions).requireDocumentAccess(actor, patientId);
    }

    @Test
    void citationsExposeCoordinatesAndIgnoreMalformedCoordinateJson() {
        AiDocumentCitationRow valid = citation("citation-1", "{\"x\":10,\"y\":20,\"width\":30,\"height\":40}");
        AiDocumentCitationRow malformed = citation("citation-2", "not-json");
        when(evidence.findCitations(documentId)).thenReturn(List.of(valid, malformed));

        AiDocumentCitationsResponseDto result = service.citations(actor, documentId);

        assertThat(result.citations()).hasSize(2);
        assertThat(result.citations().get(0).sourceBox())
                .extracting(AiDocumentCitationsResponseDto.SourceBox::x,
                        AiDocumentCitationsResponseDto.SourceBox::y,
                        AiDocumentCitationsResponseDto.SourceBox::width,
                        AiDocumentCitationsResponseDto.SourceBox::height)
                .containsExactly(10, 20, 30, 40);
        assertThat(result.citations().get(1).sourceBox()).isNull();
    }

    @Test
    void pageIssuesShortLivedSignedUrlForExistingPage() {
        AiDocumentPageRow page = mock(AiDocumentPageRow.class);
        when(page.getStorageKey()).thenReturn("rendered/doc-1/page-2.png");
        when(page.getRenderedPage()).thenReturn(true);
        when(page.getSourceBox()).thenReturn("{\"x\":104,\"y\":196,\"width\":392,\"height\":124}");
        when(page.getPageWidthPx()).thenReturn(600);
        when(page.getPageHeightPx()).thenReturn(800);
        when(evidence.findPage(documentId, 2)).thenReturn(Optional.of(page));
        when(storage.createSignedUrl(eq("rendered/doc-1/page-2.png"), any(Duration.class)))
                .thenReturn("https://storage.example/signed-page");

        var result = service.page(actor, documentId, 2);

        assertThat(result.documentId()).isEqualTo(documentId.toString());
        assertThat(result.pageNo()).isEqualTo(2);
        assertThat(result.pageUrl()).isEqualTo("https://storage.example/signed-page");
        assertThat(result.renderedPage()).isTrue();
        assertThat(result.sourceBox()).extracting(
                AiDocumentCitationsResponseDto.SourceBox::x,
                AiDocumentCitationsResponseDto.SourceBox::y,
                AiDocumentCitationsResponseDto.SourceBox::width,
                AiDocumentCitationsResponseDto.SourceBox::height
        ).containsExactly(104, 196, 392, 124);
        assertThat(result.pageWidthPx()).isEqualTo(600);
        assertThat(result.pageHeightPx()).isEqualTo(800);
        assertThat(result.expiresAt()).isAfter(java.time.Instant.now());
        verify(storage).createSignedUrl("rendered/doc-1/page-2.png", Duration.ofMinutes(5));
    }

    @Test
    void pageRejectsZeroOrNegativePageNumberBeforeStorageLookup() {
        assertThatThrownBy(() -> service.page(actor, documentId, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("페이지 요청");
    }

    @Test
    void deletedDocumentIsNotAvailableAsEvidence() {
        MedicalDocument deleted = mock(MedicalDocument.class);
        when(deleted.isDeleted()).thenReturn(true);
        when(documents.findById(documentId)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.citations(actor, documentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    private AiDocumentFactRow fact(
            String id, String type, String display, String original, String unit, int pageNo
    ) {
        AiDocumentFactRow row = mock(AiDocumentFactRow.class);
        when(row.getFactId()).thenReturn(id);
        when(row.getFactType()).thenReturn(type);
        when(row.getDisplayValue()).thenReturn(display);
        when(row.getOriginalValue()).thenReturn(original);
        when(row.getDisplayUnit()).thenReturn(unit);
        when(row.getOriginalUnit()).thenReturn(unit);
        when(row.getPageNo()).thenReturn(pageNo);
        when(row.getSourceText()).thenReturn("원문 근거");
        when(row.getAnchorId()).thenReturn("anchor-" + id);
        return row;
    }

    private AiDocumentCitationRow citation(String id, String sourceBox) {
        AiDocumentCitationRow row = mock(AiDocumentCitationRow.class);
        when(row.getCitationId()).thenReturn(id);
        when(row.getSectionId()).thenReturn("section-1");
        when(row.getSentenceId()).thenReturn("sentence-1");
        when(row.getSourceItemId()).thenReturn("item-1");
        when(row.getChunkId()).thenReturn("chunk-1");
        when(row.getPageId()).thenReturn("page-1");
        when(row.getPageNo()).thenReturn(1);
        when(row.getSourceText()).thenReturn("HbA1c 6.5%");
        when(row.getSourceBox()).thenReturn(sourceBox);
        when(row.getPageWidthPx()).thenReturn(1200);
        when(row.getPageHeightPx()).thenReturn(1800);
        when(row.getAnchorId()).thenReturn("anchor-1");
        return row;
    }
}
