package com.gyote.silvercare.health_record.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.entity.HealthRecord;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.notification.domain.SystemActivityEvent;
import com.gyote.silvercare.notification.domain.SystemActivityType;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 건강기록 작성·수정·삭제의 권한과 데이터 규칙을 처리한다. */
@Service
@Transactional
public class HealthRecordCommandService {
    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy access;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;

    public HealthRecordCommandService(
            HealthRecordRepository records,
            HealthRecordAccessPolicy access,
            JdbcTemplate jdbc,
            ApplicationEventPublisher events) {
        this.records = records;
        this.access = access;
        this.jdbc = jdbc;
        this.events = events;
    }

    /** 대상 개인과 방문의 권한을 검사한 후 본문을 저장한다. */
    public HealthRecord create(User actor, UUID patientId, String body, UUID visitId) {
        UUID target = access.requirePatient(actor, patientId);
        validateBody(body);
        validateVisit(target, visitId);
        HealthRecord saved =
                records.save(
                        HealthRecord.create(
                                target,
                                actor.getId(),
                                visitId,
                                body,
                                actor.getRole() == UserRole.CAREGIVER));
        if (actor.getRole() == UserRole.CAREGIVER) {
            events.publishEvent(
                    new SystemActivityEvent(
                            SystemActivityType.HEALTH_RECORD_CREATED,
                            target,
                            actor.getId(),
                            actor.getId(),
                            saved.getId()));
        }
        return saved;
    }

    /** 본인 프로필의 개인 또는 현재 접근 가능한 작성자가 기록을 수정한다. */
    public HealthRecord update(User actor, UUID id, String body, UUID visitId) {
        return update(actor, id, null, body, visitId);
    }

    /** 대상 개인을 바꾸는 요청은 거절하고 허용된 사용자가 본문을 수정한다. */
    public HealthRecord update(User actor, UUID id, UUID patientId, String body, UUID visitId) {
        HealthRecord record = requireWritable(actor, id);
        if (patientId != null && !patientId.equals(record.getPatientId())) {
            throw new BusinessException(HealthRecordErrorCode.INVALID_REQUEST);
        }
        validateBody(body);
        validateVisit(record.getPatientId(), visitId);
        record.update(body, visitId);
        return record;
    }

    /** 본인 프로필의 개인 또는 허용된 작성자가 기록을 소프트 삭제한다. */
    public void delete(User actor, UUID id) {
        requireWritable(actor, id).delete();
    }

    private HealthRecord requireWritable(User actor, UUID id) {
        HealthRecord record =
                records.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(HealthRecordErrorCode.NOT_FOUND));
        access.requirePatient(actor, record.getPatientId());
        // 개인 계정은 위 접근 검사에서 본인 프로필임이 확인되므로 작성자와 무관하게 관리한다.
        if (actor.getRole() != UserRole.PATIENT
                && !actor.getId().equals(record.getAuthorUserId())) {
            throw new BusinessException(HealthRecordErrorCode.AUTHOR_REQUIRED);
        }
        return record;
    }

    private void validateBody(String body) {
        if (body == null || body.isBlank() || body.length() > 2000)
            throw new BusinessException(HealthRecordErrorCode.INVALID_REQUEST);
    }

    private void validateVisit(UUID patientId, UUID visitId) {
        if (visitId == null) return;
        Integer count =
                jdbc.queryForObject(
                        "select count(*) from visits where id=? and patient_id=? and deleted_at is"
                            + " null and status not in ('CANCELLED','CANCELED')",
                        Integer.class,
                        visitId,
                        patientId);
        if (count == null || count != 1)
            throw new BusinessException(HealthRecordErrorCode.INVALID_VISIT);
    }
}
