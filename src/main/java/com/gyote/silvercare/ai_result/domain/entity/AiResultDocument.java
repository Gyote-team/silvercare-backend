package com.gyote.silvercare.ai_result.domain.entity;

import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.gyote.silvercare.ai_result.domain.ContentFormat;
import com.gyote.silvercare.ai_result.domain.DocumentSaveStatus;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_result_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiResultDocument {

    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false)
    private AiJobStatus jobStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status")
    private ResultStatus resultStatus;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "citation_json", columnDefinition = "TEXT")
    private String citationJson;
    @Column(name = "error_message")
    private String errorMessage;
    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false)
    private ContentFormat contentFormat = ContentFormat.MARKDOWN;
    @Enumerated(EnumType.STRING)
    @Column(name = "save_status", nullable = false)
    private DocumentSaveStatus saveStatus = DocumentSaveStatus.DRAFT;
    @Column(name = "saved_at")
    private Instant savedAt;
    @Column(name = "classification_source")
    private String classificationSource;
    @Column(name = "classification_confidence")
    private Double classificationConfidence;
    @Column(name = "prompt_version")
    private String promptVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    /** AI 답변 생성 대기 상태의 결과 문서를 생성한다. */
    public static AiResultDocument queued(UUID patientId, UUID requesterId, String question, QuestionType type) {
        AiResultDocument result = new AiResultDocument();
        result.id = UUID.randomUUID();
        result.requestId = UUID.randomUUID();
        result.patientId = patientId;
        result.requestedByUserId = requesterId;
        result.question = question;
        result.questionType = type == null ? QuestionType.UNSUPPORTED : type.canonical();
        result.jobStatus = AiJobStatus.QUEUED;
        result.createdAt = Instant.now();
        return result;
    }

    /** AI 답변 생성 작업을 실행 중 상태로 변경한다. */
    public void markRunning() {
        jobStatus = AiJobStatus.RUNNING;
    }

    /** 생성된 답변과 근거를 저장하고 작업을 성공 상태로 완료한다. */
    public void complete(ResultStatus status, String title, String content, String citationJson) {
        jobStatus = AiJobStatus.SUCCEEDED;
        resultStatus = status;
        this.title = title;
        this.content = content;
        this.citationJson = citationJson;
        completedAt = Instant.now();
    }

    public void recordGenerationMetadata(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public void recordClassification(QuestionType type, String source, Double confidence) {
        if (type != null) this.questionType = type.canonical();
        this.classificationSource = source;
        this.classificationConfidence = confidence;
    }

    public void saveDocument() {
        if (jobStatus != AiJobStatus.SUCCEEDED ||
                (resultStatus != ResultStatus.COMPLETE && resultStatus != ResultStatus.PARTIAL)) {
            throw new IllegalStateException("저장할 수 있는 AI 결과가 아닙니다.");
        }
        saveStatus = DocumentSaveStatus.SAVED;
        if (savedAt == null) savedAt = Instant.now();
    }

    /** 실패 사유를 저장하고 작업을 실패 상태로 완료한다. */
    public void fail(String message) {
        jobStatus = AiJobStatus.FAILED;
        errorMessage = message;
        completedAt = Instant.now();
    }
}
