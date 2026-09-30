package com.gyote.silvercare.care_relation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caregiver_links")
public class CareRelation {

    @Id
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, length = 36)
    private UUID patientId;

    @Column(name = "caregiver_user_id", nullable = false, length = 36)
    private UUID caregiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareRelationStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (requestedAt == null) {
            requestedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getCaregiverId() {
        return caregiverId;
    }

    public void setCaregiverId(UUID caregiverId) {
        this.caregiverId = caregiverId;
    }

    public CareRelationStatus getStatus() {
        return status;
    }

    public void setStatus(CareRelationStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    /** 활성 돌봄 연결을 해제한다. */
    public void revoke(Instant revokedAt) {
        this.status = CareRelationStatus.REVOKED;
        this.endedAt = revokedAt;
    }

    /** 응답 대기 중인 연결 요청을 취소한다. */
    public void cancel(Instant canceledAt) {
        this.status = CareRelationStatus.CANCELED;
        this.endedAt = canceledAt;
    }
}
