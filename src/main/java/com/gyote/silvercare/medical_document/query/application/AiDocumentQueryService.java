package com.gyote.silvercare.medical_document.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.global.type.DocumentType;
import com.gyote.silvercare.medical_document.api.dto.request.AiDocumentListRequestDto;
import com.gyote.silvercare.medical_document.domain.ExplanationSectionType;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentExplanationRepository;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentRepository;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.model.AiDocumentDetailRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentDetailView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentExplanationStatusView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionItemView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionRow;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionsView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentStatusRow;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AiDocumentQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AiDocumentRepository documents;
    private final AiDocumentExplanationRepository explanations;
    private final CareRelationPermissionService permissions;
    private final UserQueryService userQueries;

    /** 내부 사용자 식별자로 인증된 사용자를 조회한다. */
    public User requireUser(UUID userId) {
        return userQueries.requireById(userId);
    }

    /** 사용자의 권한 범위에 포함된 AI 문서 목록을 조회한다. */
    public Page<AiDocumentListView> listDocuments(User actor, AiDocumentListRequestDto request) {
        UUID requestedPatientId = parseOptionalUuid(request.patientId());
        UUID targetPatientId = permissions.resolveTargetPatient(actor, requestedPatientId);
        UUID visitId = parseOptionalUuid(request.visitId());
        String documentType = parseDocumentType(request.docType());
        String documentStatus = parseDocumentStatus(request.status());
        Pageable pageable = pageable(request.page(), request.size());

        return documents.findCompletedDocuments(
                targetPatientId,
                visitId,
                documentType,
                documentStatus,
                pageable
        ).map(this::toListView);
    }

    /** 문서의 AI 설명 상세 정보를 권한 검증 후 조회한다. */
    public AiDocumentDetailView getDocument(User actor, UUID documentId) {
        requireDocumentPermission(actor, documentId);
        AiDocumentDetailRow row = explanations.findDetailByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
        requireAnalysisCompleted(row.getAnalysisStatus());
        requireExplanationSucceeded(row.getJobStatus());

        return new AiDocumentDetailView(
                parseUuid(row.getDocumentId()),
                row.getDocumentName(),
                enumValue(DocumentType.class, row.getDocumentType(), AiDocumentErrorCode.INVALID_FILTER),
                parseUuid(row.getVisitId()),
                enumValue(DocumentStatus.class, row.getDocumentStatus(), AiDocumentErrorCode.INVALID_FILTER),
                enumValue(AiJobStatus.class, row.getJobStatus(), AiDocumentErrorCode.INVALID_FILTER),
                nullableEnum(ResultStatus.class, row.getResultStatus()),
                row.getTitle(),
                row.getContent(),
                parseDbInstant(row.getCreatedAt()),
                defaultLong(row.getSectionCount()),
                defaultLong(row.getCitationCount())
        );
    }

    /** 문서의 AI 설명 섹션과 섹션별 항목을 조회한다. */
    public AiDocumentSectionsView getSections(
            User actor,
            UUID documentId,
            String rawSectionType
    ) {
        requireDocumentPermission(actor, documentId);
        requireExplanationSucceeded(statusRow(documentId).getJobStatus());
        String sectionType = parseSectionType(rawSectionType);
        List<AiDocumentSectionRow> rows = explanations.findSectionRows(documentId, sectionType);

        Map<UUID, List<AiDocumentSectionRow>> grouped = new LinkedHashMap<>();
        for (AiDocumentSectionRow row : rows) {
            grouped.computeIfAbsent(parseUuid(row.getSectionId()), ignored -> new ArrayList<>()).add(row);
        }

        List<AiDocumentSectionView> sections = grouped.values().stream()
                .map(this::toSectionView)
                .toList();
        return new AiDocumentSectionsView(documentId, sections);
    }

    /** 문서의 AI 설명 작업 상태와 재시도 가능 여부를 조회한다. */
    public AiDocumentExplanationStatusView getExplanationStatus(User actor, UUID documentId) {
        requireDocumentPermission(actor, documentId);
        AiDocumentStatusRow row = statusRow(documentId);
        AiJobStatus jobStatus = enumValue(
                AiJobStatus.class,
                row.getJobStatus(),
                AiDocumentErrorCode.INVALID_FILTER
        );
        ResultStatus resultStatus = nullableEnum(ResultStatus.class, row.getResultStatus());
        String detailUrl = jobStatus == AiJobStatus.SUCCEEDED
                ? "/api/ai-documents/" + documentId
                : null;

        return new AiDocumentExplanationStatusView(
                parseUuid(row.getDocumentId()),
                jobStatus,
                resultStatus,
                row.getCurrentStep(),
                row.getProgress(),
                parseDbInstant(row.getCompletedAt()),
                detailUrl,
                row.getFailedStep(),
                row.getErrorCode(),
                Boolean.TRUE.equals(row.getRetryable()),
                "/api/documents/" + documentId
        );
    }

    /** 조회 결과 한 건을 목록 조회 모델로 변환한다. */
    private AiDocumentListView toListView(AiDocumentListRow row) {
        return new AiDocumentListView(
                parseUuid(row.getDocumentId()),
                row.getDocumentName(),
                enumValue(DocumentType.class, row.getDocumentType(), AiDocumentErrorCode.INVALID_FILTER),
                parseUuid(row.getVisitId()),
                row.getVisitedOn(),
                parseDbInstant(row.getCreatedAt()),
                row.getAuthorName(),
                enumValue(DocumentStatus.class, row.getDocumentStatus(), AiDocumentErrorCode.INVALID_FILTER),
                enumValue(AiJobStatus.class, row.getJobStatus(), AiDocumentErrorCode.INVALID_FILTER),
                nullableEnum(ResultStatus.class, row.getResultStatus())
        );
    }

    /** 동일 섹션의 조회 결과 행들을 하나의 섹션 모델로 묶는다. */
    private AiDocumentSectionView toSectionView(List<AiDocumentSectionRow> rows) {
        AiDocumentSectionRow first = rows.get(0);
        List<AiDocumentSectionItemView> items = rows.stream()
                .filter(row -> row.getSentenceId() != null)
                .map(row -> new AiDocumentSectionItemView(
                        parseUuid(row.getSentenceId()),
                        parseNullableUuid(row.getSourceItemId()),
                        row.getLabel(),
                        row.getDisplayValue(),
                        row.getUnit(),
                        Boolean.TRUE.equals(row.getHasSource()),
                        parseNullableUuid(row.getCitationId())
                ))
                .toList();
        return new AiDocumentSectionView(
                parseUuid(first.getSectionId()),
                enumValue(
                        ExplanationSectionType.class,
                        first.getSectionType(),
                        AiDocumentErrorCode.INVALID_SECTION_TYPE
                ),
                first.getSectionTitle(),
                items
        );
    }

    /** 문서가 존재하고 현재 사용자가 해당 환자의 문서를 볼 수 있는지 검증한다. */
    private void requireDocumentPermission(User actor, UUID documentId) {
        String patientId = documents.findPatientByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND))
                .getPatientId();
        permissions.requireDocumentAccess(actor, parseUuid(patientId));
    }

    /** 문서의 최신 분석 및 AI 설명 상태 행을 조회한다. */
    private AiDocumentStatusRow statusRow(UUID documentId) {
        return explanations.findStatusByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    /** 원본 문서 분석이 완료된 상태인지 검증한다. */
    private static void requireAnalysisCompleted(String rawStatus) {
        if (!"SUCCEEDED".equals(rawStatus)) {
            throw new BusinessException(AiDocumentErrorCode.ANALYSIS_NOT_COMPLETED);
        }
    }

    /** AI 설명 작업이 성공적으로 완료된 상태인지 검증한다. */
    private static void requireExplanationSucceeded(String rawStatus) {
        if (!"SUCCEEDED".equals(rawStatus)) {
            throw new BusinessException(AiDocumentErrorCode.EXPLANATION_NOT_READY);
        }
    }

    /** 페이지 번호와 크기를 검증하고 페이지 요청 객체를 생성한다. */
    private static Pageable pageable(int page, int size) {
        int actualSize = size == 0 ? DEFAULT_PAGE_SIZE : size;
        if (page < 0 || actualSize < 1 || actualSize > MAX_PAGE_SIZE) {
            throw new BusinessException(AiDocumentErrorCode.INVALID_PAGE_REQUEST);
        }
        return PageRequest.of(page, actualSize);
    }

    /** 선택적으로 전달된 문자열 UUID를 변환한다. */
    private static UUID parseOptionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(AiDocumentErrorCode.INVALID_FILTER);
        }
    }

    /** 문서 유형 필터를 검증하고 DB 조회용 값으로 변환한다. */
    private static String parseDocumentType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return enumValue(DocumentType.class, value, AiDocumentErrorCode.INVALID_FILTER).name();
    }

    /** 문서 상태 필터를 검증하고 계약 상태를 DB 상태로 변환한다. */
    private static String parseDocumentStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("COMPLETED".equalsIgnoreCase(value)) {
            return DocumentStatus.READY.name();
        }
        return enumValue(DocumentStatus.class, value, AiDocumentErrorCode.INVALID_FILTER).name();
    }

    /** 섹션 유형 필터를 검증하고 DB 조회용 값으로 변환한다. */
    private static String parseSectionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return enumValue(ExplanationSectionType.class, value, AiDocumentErrorCode.INVALID_SECTION_TYPE).name();
    }

    /** 문자열을 지정된 enum 값으로 변환하고 잘못된 값은 계약 오류로 처리한다. */
    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String rawValue,
            AiDocumentErrorCode errorCode
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BusinessException(errorCode);
        }
        try {
            return Enum.valueOf(type, rawValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(errorCode);
        }
    }

    /** 비어 있는 문자열은 null로, 값이 있으면 enum으로 변환한다. */
    private static <E extends Enum<E>> E nullableEnum(Class<E> type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, rawValue.toUpperCase());
    }

    /** null인 집계 결과를 0으로 치환한다. */
    private static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /** PostgreSQL·H2 native 조회 시간 문자열을 공통 Instant로 변환한다. */
    private static Instant parseDbInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', 'T');
        if (normalized.matches(".*[+-]\\d{2}$")) {
            normalized = normalized + ":00";
        }
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        }
    }

    /** 필수 UUID 문자열을 변환하고 변환 실패 시 문서 없음 오류를 발생시킨다. */
    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    /** nullable UUID 문자열을 null을 보존한 채 변환한다. */
    private static UUID parseNullableUuid(String value) {
        return value == null || value.isBlank() ? null : parseUuid(value);
    }
}
