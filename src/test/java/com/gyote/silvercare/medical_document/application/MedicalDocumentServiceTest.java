package com.gyote.silvercare.medical_document.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentDeletedEvent;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentPage;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentView;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MedicalDocumentServiceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private MedicalDocumentRepository documents;

    @Autowired
    private TestEntityManager em;

    private UserAccountService accounts;
    private MedicalDocumentQueryService queries;
    private MedicalDocumentCommandService commands;
    private List<Object> publishedEvents;

    private User patient;
    private User caregiver;
    private User strangerCaregiver;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        accounts = new UserAccountService(users, patients);
        MedicalDocumentAccessPolicy accessPolicy = new MedicalDocumentAccessPolicy(relations, patients);
        queries = new MedicalDocumentQueryService(
                documents, users, new UserQueryService(users), patients, accessPolicy,
                (objectKey, ttl) -> "https://storage.test/" + objectKey);
        publishedEvents = new ArrayList<>();
        commands = new MedicalDocumentCommandService(documents, accessPolicy, publishedEvents::add);

        patient = createPatient("kakao-soonja", "김순자");
        caregiver = createCaregiver("kakao-minji", "김민지");
        strangerCaregiver = createCaregiver("kakao-stranger", "남보호");
        connect(caregiver, patient);
        patientId = patientIdOf(patient);
    }

    @Test
    void patientListsOnlyOwnDocumentsNewestFirst() {
        MedicalDocument first = saveDocument(patientId, patient);
        MedicalDocument second = saveDocument(patientId, patient);
        saveDocument(patientIdOf(createPatient("kakao-younghee", "박영희")), caregiver);

        MedicalDocumentPage page = queries.list(patient, null, null, null, null);

        assertThat(idsOf(page)).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(page.items())
                .isSortedAccordingTo(Comparator.comparing(MedicalDocumentView::createdAt).reversed());
    }

    @Test
    void activeCaregiverListsConnectedPatientDocuments() {
        MedicalDocument document = saveDocument(patientId, patient);

        MedicalDocumentPage page = queries.list(caregiver, patientId, null, null, null);

        assertThat(idsOf(page)).containsExactly(document.getId());
    }

    @Test
    void unconnectedCaregiverCannotListDocuments() {
        saveDocument(patientId, patient);

        assertErrorCode(() -> queries.list(strangerCaregiver, patientId, null, null, null),
                MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
    }

    @Test
    void caregiverWithoutPatientIdIsRejected() {
        assertErrorCode(() -> queries.list(caregiver, null, null, null, null),
                MedicalDocumentErrorCode.PATIENT_ID_REQUIRED);
    }

    @Test
    void cursorPaginationReturnsRemainingDocumentsWithoutDuplicates() {
        saveDocument(patientId, patient);
        saveDocument(patientId, patient);
        saveDocument(patientId, patient);

        MedicalDocumentPage firstPage = queries.list(patient, null, null, null, 2);
        MedicalDocumentPage secondPage = queries.list(patient, null, null, firstPage.nextCursor(), 2);

        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.nextCursor()).isNotNull();
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(idsOf(firstPage)).doesNotContainAnyElementsOf(idsOf(secondPage));
    }

    @Test
    void malformedCursorIsRejected() {
        assertErrorCode(() -> queries.list(patient, null, null, "not-a-cursor", null),
                MedicalDocumentErrorCode.INVALID_CURSOR);
    }

    @Test
    void detailHasSignedUrlButListItemsDoNot() {
        MedicalDocument document = saveDocument(patientId, patient);

        MedicalDocumentView detail = queries.get(patient, document.getId());
        MedicalDocumentView listItem = queries.list(patient, null, null, null, null).items().get(0);

        assertThat(detail.signedUrl()).isEqualTo("https://storage.test/" + document.getObjectKey());
        assertThat(listItem.signedUrl()).isNull();
    }

    @Test
    void patientDeleteHidesDocumentAndPublishesEvent() {
        MedicalDocument document = saveDocument(patientId, patient);

        commands.delete(patient, document.getId());

        assertErrorCode(() -> queries.get(patient, document.getId()),
                MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
        assertThat(queries.list(patient, null, null, null, null).items()).isEmpty();
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(MedicalDocumentDeletedEvent.class, event -> {
                    assertThat(event.documentId()).isEqualTo(document.getId());
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.deletedBy()).isEqualTo(patient.getId());
                });
    }

    @Test
    void deletingAlreadyDeletedDocumentIsNotFound() {
        MedicalDocument document = saveDocument(patientId, patient);
        commands.delete(patient, document.getId());

        assertErrorCode(() -> commands.delete(patient, document.getId()),
                MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void caregiverCannotDeleteDocumentUploadedByPatient() {
        MedicalDocument document = saveDocument(patientId, patient);

        assertErrorCode(() -> commands.delete(caregiver, document.getId()),
                MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
    }

    @Test
    void activeCaregiverDeletesOwnUpload() {
        MedicalDocument document = saveDocument(patientId, caregiver);

        commands.delete(caregiver, document.getId());

        assertThat(queries.list(caregiver, patientId, null, null, null).items()).isEmpty();
    }

    private User createPatient(String kakaoId, String name) {
        return createUser(kakaoId, name, UserRole.PATIENT);
    }

    private User createCaregiver(String kakaoId, String name) {
        return createUser(kakaoId, name, UserRole.CAREGIVER);
    }

    private User createUser(String kakaoId, String name, UserRole role) {
        return accounts.chooseRole(accounts.loginOrRegister(kakaoId, name).getKakaoId(), role);
    }

    private void connect(User caregiver, User patient) {
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients);
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        cares.accept(patient, requested.getId());
    }

    private UUID patientIdOf(User user) {
        return patients.findByUserId(user.getId()).orElseThrow().getId();
    }

    /** 저장 후 flush·clear 해서 이후 조회가 DB 값(마이크로초 단위 createdAt)을 읽게 합니다. */
    private MedicalDocument saveDocument(UUID patientId, User uploader) {
        MedicalDocument saved = documents.save(MedicalDocument.uploaded(
                patientId, UUID.randomUUID(), uploader.getId(),
                "검사결과.pdf", "documents/" + UUID.randomUUID(), "application/pdf", 1024L, null));
        em.flush();
        em.clear();
        return saved;
    }

    private static List<UUID> idsOf(MedicalDocumentPage page) {
        return page.items().stream().map(MedicalDocumentView::documentId).toList();
    }

    private static void assertErrorCode(ThrowingCallable call, MedicalDocumentErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
