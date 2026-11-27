package com.gyote.silvercare.schedule.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.schedule.domain.RepeatFrequency;
import com.gyote.silvercare.schedule.domain.ScheduleAccessPolicy;
import com.gyote.silvercare.schedule.domain.ScheduleStatus;
import com.gyote.silvercare.schedule.domain.entity.Schedule;
import com.gyote.silvercare.schedule.domain.repository.ScheduleRepository;
import com.gyote.silvercare.schedule.error.ScheduleErrorCode;
import com.gyote.silvercare.schedule.query.model.SchedulePageView;
import com.gyote.silvercare.schedule.query.model.ScheduleRow;
import com.gyote.silvercare.user.domain.User;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ScheduleQueryService {
    private final ScheduleRepository schedules;
    private final ScheduleAccessPolicy access;

    public ScheduleQueryService(ScheduleRepository schedules, ScheduleAccessPolicy access) {
        this.schedules = schedules;
        this.access = access;
    }

    /** 일정 시간대·반복 규칙을 생성 시각과 무관하게 시작일 순서로 조회한다. */
    public SchedulePageView list(User actor, UUID patientId, int page, int size) {
        UUID patient = access.requirePatient(actor, patientId);
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        var result =
                schedules.findByPatientIdAndDeletedAtIsNull(
                        patient, PageRequest.of(page, size, Sort.by("startsAt", "id")));
        return new SchedulePageView(
                result.getContent().stream().map(this::row).toList(),
                page,
                size,
                result.getTotalElements());
    }

    /** 문서 삭제와 관계없이 확정 일정의 현재 규칙과 버전을 반환한다. */
    public ScheduleRow detail(User actor, UUID id) {
        var schedule =
                schedules
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.NOT_FOUND));
        access.requirePatient(actor, schedule.getPatientId());
        return row(schedule);
    }

    private ScheduleRow row(Schedule s) {
        return new ScheduleRow(
                s.getId(),
                s.getPatientId(),
                s.getActionItemId(),
                s.getTitle(),
                s.getScheduleType(),
                s.getStartsAt(),
                s.getEndsAt(),
                s.getStatus(),
                s.getRepeatFrequency(),
                s.getRepeatInterval(),
                s.getRepeatUntil(),
                nextOccurrence(s, Instant.now()),
                s.getVersion());
    }

    /** 한국 시간의 같은 시각을 유지하며 종료일 이내의 다음 복약 일시를 계산한다. */
    public static Instant nextOccurrence(Schedule s, Instant now) {
        if (s.getStatus() != ScheduleStatus.SCHEDULED) return null;
        if (s.getRepeatFrequency() == RepeatFrequency.NONE) return s.getStartsAt();
        ZoneId zone = ZoneId.of("Asia/Seoul");
        var start = s.getStartsAt().atZone(zone);
        long step =
                (long) s.getRepeatInterval()
                        * (s.getRepeatFrequency() == RepeatFrequency.WEEKLY ? 7 : 1);
        long days =
                Math.max(
                        0,
                        ChronoUnit.DAYS.between(
                                start.toLocalDate(), now.atZone(zone).toLocalDate()));
        var next = start.plusDays((days / step) * step);
        if (next.toInstant().isBefore(now)) next = next.plusDays(step);
        return next.toLocalDate().isAfter(s.getRepeatUntil()) ? null : next.toInstant();
    }
}
