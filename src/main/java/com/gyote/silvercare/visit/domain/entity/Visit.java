package com.gyote.silvercare.visit.domain.entity;

import com.gyote.silvercare.visit.domain.VisitStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 개인 건강 프로필에 귀속되는 병원 방문. 취소 시 하위 자료와 참조를 유지한다. */
@Entity
@Table(name = "visits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {
    @Id private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "hospital_name", length = 200)
    private String hospitalName;

    @Column(length = 100)
    private String department;

    @Column(name = "visited_on", nullable = false)
    private LocalDate visitedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'SCHEDULED'")
    private VisitStatus status;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @ColumnDefault("0")
    private long version;

    /** 대상 개인을 고정하고 방문을 생성한다. */
    public static Visit create(
            UUID patientId,
            LocalDate visitedOn,
            String hospitalName,
            String department,
            String memo,
            VisitStatus status) {
        Visit visit = new Visit();
        visit.id = UUID.randomUUID();
        visit.patientId = patientId;
        visit.update(visitedOn, hospitalName, department, memo, status);
        visit.createdAt = visit.updatedAt;
        return visit;
    }

    /** 대상 개인과 참조를 유지하면서 방문 정보를 변경한다. */
    public void update(
            LocalDate visitedOn,
            String hospitalName,
            String department,
            String memo,
            VisitStatus status) {
        this.visitedOn = visitedOn;
        this.hospitalName = hospitalName;
        this.department = department;
        this.memo = memo;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** 취소 상태로 전환한다. 재요청은 같은 상태를 유지한다. */
    public void cancel() {
        if (status != VisitStatus.CANCELED) {
            status = VisitStatus.CANCELED;
            updatedAt = Instant.now();
        }
    }
}
