package com.gyote.silvercare.medical_document.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentPage;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentView;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read-only medical-document queries. */
@Service
@Transactional(readOnly = true)
public class MedicalDocumentQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(5);
    private static final String CURSOR_DELIMITER = "|";

    private final MedicalDocumentRepository documents;
    private final UserRepository users;
    private final UserQueryService userQueries;
    private final PatientRepository patients;
    private final MedicalDocumentAccessPolicy accessPolicy;
    private final DocumentStoragePort storage;

    public MedicalDocumentQueryService(
            MedicalDocumentRepository documents,
            UserRepository users,
            UserQueryService userQueries,
            PatientRepository patients,
            MedicalDocumentAccessPolicy accessPolicy,
            DocumentStoragePort storage
    ) {
        this.documents = documents;
        this.users = users;
        this.userQueries = userQueries;
        this.patients = patients;
        this.accessPolicy = accessPolicy;
        this.storage = storage;
    }

    public User requireUser(String kakaoId) {
        return userQueries.requireByKakaoId(kakaoId);
    }

    public MedicalDocumentPage list(User me, UUID patientId, UUID visitId, String cursor, Integer size) {
        UUID targetId = resolvePatientId(me, patientId);
        accessPolicy.checkReadable(me, targetId);

        int pageSize = pageSize(size);
        List<MedicalDocument> rows = fetch(targetId, visitId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasNext = rows.size() > pageSize;
        List<MedicalDocument> page = hasNext ? rows.subList(0, pageSize) : rows;

        Map<UUID, User> authorsById = authorsById(page);
        List<MedicalDocumentView> items = page.stream()
                .map(document -> toView(document, authorsById, null))
                .toList();
        String nextCursor = hasNext ? encodeCursor(page.get(page.size() - 1)) : null;
        return new MedicalDocumentPage(items, nextCursor);
    }

    public MedicalDocumentView get(User me, UUID documentId) {
        MedicalDocument document = documents.findByIdAndDocumentStatusNot(documentId, DocumentStatus.DELETED)
                .orElseThrow(() -> new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND));
        accessPolicy.checkReadable(me, document.getPatientId());
        String signedUrl = storage.createSignedUrl(document.getObjectKey(), SIGNED_URL_TTL);
        return toView(document, authorsById(List.of(document)), signedUrl);
    }

    private UUID resolvePatientId(User me, UUID patientId) {
        if (patientId != null) {
            return patientId;
        }
        if (me.getRole() == UserRole.PATIENT) {
            return patients.findByUserId(me.getId())
                    .map(Patient::getId)
                    .orElseThrow(() -> new BusinessException(MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED));
        }
        throw new BusinessException(MedicalDocumentErrorCode.PATIENT_ID_REQUIRED);
    }

    private static int pageSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(MAX_SIZE, size));
    }

    private List<MedicalDocument> fetch(UUID patientId, UUID visitId, String cursor, Pageable pageable) {
        DocumentStatus excluded = DocumentStatus.DELETED;
        if (cursor == null || cursor.isBlank()) {
            return visitId == null
                    ? documents.findFirstPage(patientId, excluded, pageable)
                    : documents.findFirstPageByVisit(patientId, visitId, excluded, pageable);
        }
        Cursor decoded = decodeCursor(cursor);
        return visitId == null
                ? documents.findNextPage(patientId, excluded, decoded.createdAt(), decoded.id(), pageable)
                : documents.findNextPageByVisit(patientId, visitId, excluded, decoded.createdAt(), decoded.id(), pageable);
    }

    private Map<UUID, User> authorsById(List<MedicalDocument> rows) {
        Set<UUID> uploaderIds = rows.stream()
                .map(MedicalDocument::getUploaderId)
                .collect(Collectors.toSet());
        return users.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static MedicalDocumentView toView(MedicalDocument document, Map<UUID, User> authorsById, String signedUrl) {
        User uploader = authorsById.get(document.getUploaderId());
        MedicalDocumentView.Author author = uploader == null
                ? new MedicalDocumentView.Author("이용자", null)
                : new MedicalDocumentView.Author(uploader.getName(), uploader.getRole().name());
        return new MedicalDocumentView(
                document.getId(),
                document.getVisitId(),
                document.getPatientId(),
                document.getDocumentName(),
                document.getDocumentType(),
                null,
                author,
                document.getDocumentStatus(),
                null,
                null,
                document.getStatusChangedAt(),
                false,
                signedUrl,
                document.getCreatedAt()
        );
    }

    private static String encodeCursor(MedicalDocument last) {
        String raw = last.getCreatedAt() + CURSOR_DELIMITER + last.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int split = raw.indexOf(CURSOR_DELIMITER);
            if (split < 0) {
                throw new BusinessException(MedicalDocumentErrorCode.INVALID_CURSOR);
            }
            return new Cursor(Instant.parse(raw.substring(0, split)), UUID.fromString(raw.substring(split + 1)));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new BusinessException(MedicalDocumentErrorCode.INVALID_CURSOR);
        }
    }

    private record Cursor(Instant createdAt, UUID id) {
    }
}
