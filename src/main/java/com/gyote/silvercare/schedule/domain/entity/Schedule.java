package com.gyote.silvercare.schedule.domain.entity;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.schedule.domain.RepeatFrequency;
import com.gyote.silvercare.schedule.domain.ScheduleInput;
import com.gyote.silvercare.schedule.domain.ScheduleStatus;
import com.gyote.silvercare.schedule.domain.ScheduleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** 한 시간대의 일정과 반복 규칙. 수정·완료·삭제는 반복 전체에 적용한다. */
@Entity
@Table(
        name = "schedules",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "ux_schedules_candidate_slot",
                        columnNames = {"action_item_id", "slot_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {
    @Id private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "action_item_id")
    private UUID actionItemId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_frequency", nullable = false, length = 10)
    private RepeatFrequency repeatFrequency;

    @Column(name = "repeat_interval", nullable = false)
    private int repeatInterval;

    @Column(name = "repeat_until")
    private LocalDate repeatUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version private long version;

    /** 검증된 입력으로 수동 일정 또는 승인 후보의 한 시간대를 생성한다. */
    public static Schedule create(UUID patientId, UUID candidateId, int slot, ScheduleInput input) {
        Schedule row = new Schedule();
        row.id = UUID.randomUUID();
        row.patientId = patientId;
        row.actionItemId = candidateId;
        row.slotIndex = slot;
        row.status = ScheduleStatus.SCHEDULED;
        row.createdAt = Instant.now();
        row.apply(input);
        return row;
    }

    /** 모든 쓰기 경로에서 동일하게 길이·날짜·복약 반복 규칙을 검증한다. */
    public static void validate(ScheduleInput input) {
        if (input == null
                || input.title() == null
                || input.title().isBlank()
                || input.title().trim().length() > 200
                || input.scheduleType() == null
                || input.startsAt() == null
                || input.repeatFrequency() == null
                || input.repeatInterval() == null)
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        Instant earliest = Instant.parse("1999-12-31T15:00:00Z");
        Instant latest = Instant.parse("2100-12-31T15:00:00Z");
        if (input.startsAt().isBefore(earliest) || !input.startsAt().isBefore(latest))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (input.endsAt() != null
                && (!input.endsAt().isAfter(input.startsAt()) || !input.endsAt().isBefore(latest)))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        LocalDate start = input.startsAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        if (start.getYear() < 2000 || start.getYear() > 2100)
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (input.repeatFrequency() == RepeatFrequency.NONE) {
            if (input.repeatInterval() != 1 || input.repeatUntil() != null)
                throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        } else if (input.scheduleType() != ScheduleType.MEDICATION
                || input.repeatInterval() < 1
                || input.repeatInterval() > 30
                || input.repeatUntil() == null
                || input.repeatUntil().isBefore(start)
                || input.repeatUntil().isAfter(start.plusYears(1))) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    /** 기존 소유자·후보 참조를 유지하면서 반복 전체의 내용과 상태를 수정한다. */
    public void update(ScheduleInput input, ScheduleStatus status) {
        if (status == null) throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        apply(input);
        this.status = status;
    }

    /** 반복 전체를 소프트 삭제하며 후보의 승인 이력은 보존한다. */
    public void delete() {
        deletedAt = Instant.now();
        updatedAt = deletedAt;
        status = ScheduleStatus.CANCELLED;
    }

    private void apply(ScheduleInput input) {
        validate(input);
        title = input.title().trim();
        scheduleType = input.scheduleType();
        startsAt = input.startsAt();
        endsAt = input.endsAt();
        repeatFrequency = input.repeatFrequency();
        repeatInterval = input.repeatInterval();
        repeatUntil = input.repeatUntil();
        updatedAt = Instant.now();
    }
}
