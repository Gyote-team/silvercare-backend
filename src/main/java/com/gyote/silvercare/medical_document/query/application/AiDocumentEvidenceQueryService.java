package com.gyote.silvercare.medical_document.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentCitationsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentFactsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentPageResponseDto;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentEvidenceRepository;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentRepository;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.model.AiDocumentCitationRow;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 설명 값과 원문 인용의 교차 검증 결과를 제공하는 조회 서비스입니다. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AiDocumentEvidenceQueryService {

    /** 서명 URL의 노출 시간을 짧게 제한합니다. */
    private static final Duration PAGE_URL_TTL = Duration.ofMinutes(5);

    private final AiDocumentRepository documents;
    private final AiDocumentEvidenceRepository evidence;
    private final CareRelationPermissionService permissions;
    private final DocumentStoragePort storage;
    private final ObjectMapper objectMapper;

    /** 문서의 설명값과 원문값 대조 결과를 조회합니다. */
    public AiDocumentFactsResponseDto facts(User actor, UUID documentId) {
        requirePermission(actor, documentId);
        List<AiDocumentFactsResponseDto.FactItem> items = evidence.findFacts(documentId)
                .stream()
                .map(row -> toFactItem(row))
                .toList();
        return new AiDocumentFactsResponseDto(documentId.toString(), items);
    }

    /** 문서의 위치가 확인된 원문 인용만 조회합니다. */
    public AiDocumentCitationsResponseDto citations(User actor, UUID documentId, UUID sectionId) {
        requirePermission(actor, documentId);
        List<AiDocumentCitationsResponseDto.CitationItem> citations = evidence.findCitations(documentId, sectionId)
                .stream()
                .map(this::citation)
                .toList();
        return new AiDocumentCitationsResponseDto(documentId.toString(), citations);
    }

    public AiDocumentCitationsResponseDto citations(User actor, UUID documentId) {
        requirePermission(actor, documentId);
        List<AiDocumentCitationsResponseDto.CitationItem> citations = evidence.findCitations(documentId)
                .stream()
                .map(this::citation)
                .toList();
        return new AiDocumentCitationsResponseDto(documentId.toString(), citations);
    }

    /** 페이지 렌더링 파일 또는 원본 문서의 짧은 서명 URL을 생성합니다. */
    public AiDocumentPageResponseDto page(User actor, UUID documentId, int pageNo, String anchorId) {
        if (pageNo < 1) {
            throw new BusinessException(AiDocumentErrorCode.INVALID_PAGE_REQUEST);
        }
        requirePermission(actor, documentId);
        if (anchorId != null && !anchorId.isBlank() && !evidence.existsAnchorInDocument(documentId, anchorId)) {
            throw new BusinessException(AiDocumentErrorCode.PAGE_NOT_FOUND);
        }
        var row = evidence.findPage(documentId, pageNo, anchorId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.PAGE_NOT_FOUND));
        Instant expiresAt = Instant.now().plus(PAGE_URL_TTL);
        return new AiDocumentPageResponseDto(
                documentId.toString(),
                pageNo,
                storage.createSignedUrl(row.getStorageKey(), PAGE_URL_TTL),
                expiresAt,
                Boolean.TRUE.equals(row.getRenderedPage()),
                anchorId,
                sourceBox(row.getSourceBox()),
                row.getPageWidthPx(),
                row.getPageHeightPx()
        );
    }

    public AiDocumentPageResponseDto page(User actor, UUID documentId, int pageNo) {
        if (pageNo < 1) {
            throw new BusinessException(AiDocumentErrorCode.INVALID_PAGE_REQUEST);
        }
        requirePermission(actor, documentId);
        var row = evidence.findPage(documentId, pageNo)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.PAGE_NOT_FOUND));
        Instant expiresAt = Instant.now().plus(PAGE_URL_TTL);
        return new AiDocumentPageResponseDto(
                documentId.toString(), pageNo,
                storage.createSignedUrl(row.getStorageKey(), PAGE_URL_TTL), expiresAt,
                Boolean.TRUE.equals(row.getRenderedPage()), null,
                sourceBox(row.getSourceBox()), row.getPageWidthPx(), row.getPageHeightPx()
        );
    }

    /** 원문과 일치하지 않는 설명값은 절대 화면에 전달하지 않습니다. */
    private AiDocumentFactsResponseDto.FactItem toFactItem(
            com.gyote.silvercare.medical_document.query.model.AiDocumentFactRow row
    ) {
        boolean matched = same(row.getDisplayValue(), row.getOriginalValue())
                && same(row.getDisplayUnit(), row.getOriginalUnit());
        return new AiDocumentFactsResponseDto.FactItem(
                row.getFactId(),
                factType(row.getFactType()),
                matched ? row.getDisplayValue() : null,
                row.getOriginalValue(),
                row.getDisplayUnit(),
                row.getOriginalUnit(),
                matched ? "MATCHED" : "MISMATCHED",
                row.getPageNo(),
                row.getSourceText(),
                row.getAnchorId()
        );
    }

    /** DB 행을 공통 인용 응답 계약으로 변환합니다. */
    private AiDocumentCitationsResponseDto.CitationItem citation(AiDocumentCitationRow row) {
        return new AiDocumentCitationsResponseDto.CitationItem(
                row.getCitationId(), row.getSectionId(), row.getSentenceId(),
                row.getSourceItemId(), row.getChunkId(), row.getPageId(),
                row.getPageNo(), row.getSourceText(), sourceBox(row.getSourceBox()),
                row.getPageWidthPx(), row.getPageHeightPx(), row.getAnchorId()
        );
    }

    /** JSONB 좌표를 계약 DTO로 변환하고 잘못된 좌표는 숨깁니다. */
    private AiDocumentCitationsResponseDto.SourceBox sourceBox(String rawSourceBox) {
        if (rawSourceBox == null || rawSourceBox.isBlank()) {
            return null;
        }
        try {
            JsonNode box = objectMapper.readTree(rawSourceBox);
            return new AiDocumentCitationsResponseDto.SourceBox(
                    integer(box, "x"),
                    integer(box, "y"),
                    integer(box, "width"),
                    integer(box, "height")
            );
        } catch (Exception exception) {
            log.debug("원문 좌표 JSON 파싱에 실패했습니다.", exception);
            return null;
        }
    }

    /** 좌표 JSON의 숫자 필드를 안전하게 읽습니다. */
    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.intValue() : null;
    }

    /** 문서 존재 여부와 환자·보호자 접근 권한을 함께 검증합니다. */
    private void requirePermission(User actor, UUID documentId) {
        var document = documents.findById(documentId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
        permissions.requireDocumentAccess(actor, document.getPatientId());
    }

    /** 공백 차이만 허용하고, 값 누락은 불일치로 처리합니다. */
    private static boolean same(String left, String right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.replaceAll("\\s+", "").equals(right.replaceAll("\\s+", ""));
    }

    /** 화면 계약에서 사용하는 검사값 이름으로 변환합니다. */
    private static String factType(String value) {
        return "LAB_VALUE".equals(value) ? "TEST_VALUE" : value;
    }
}
