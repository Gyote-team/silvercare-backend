package com.gyote.silvercare.visit.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.visit.domain.VisitAccessPolicy;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;
import com.gyote.silvercare.visit.error.VisitErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** 권한·날짜·상태를 검증하며 방문 및 하위 자료 참조를 보존한다. */
@Service
@Transactional
public class VisitCommandService {
    private final VisitRepository visits;
    private final VisitAccessPolicy access;

    public VisitCommandService(VisitRepository visits, VisitAccessPolicy access) {
        this.visits = visits;
        this.access = access;
    }

    /** 본인 또는 ACTIVE 보호자가 방문을 등록한다. */
    public Visit create(
            User actor,
            UUID patientId,
            LocalDate date,
            String hospital,
            String department,
            String memo,
            VisitStatus status) {
        UUID target = access.requirePatient(actor, patientId);
        validate(date, hospital, department, memo, status);
        return visits.save(
                Visit.create(target, date, trim(hospital), trim(department), memo, status));
    }

    /** 개인 프로필 재귀속을 금지하고 방문 내용을 수정한다. */
    public Visit update(
            User actor,
            UUID id,
            UUID patientId,
            LocalDate date,
            String hospital,
            String department,
            String memo,
            VisitStatus status) {
        Visit visit = require(actor, id);
        if (patientId != null && !patientId.equals(visit.getPatientId()))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (visit.getStatus() == VisitStatus.CANCELED)
            throw new BusinessException(VisitErrorCode.CANCELED_IMMUTABLE);
        validate(date, hospital, department, memo, status);
        visit.update(date, trim(hospital), trim(department), memo, status);
        return visit;
    }

    /** 물리 삭제 없이 취소 상태로 전환한다. 연결된 자료는 그대로 유지한다. */
    public void cancel(User actor, UUID id) {
        require(actor, id).cancel();
    }

    private Visit require(User actor, UUID id) {
        Visit visit =
                visits.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(VisitErrorCode.NOT_FOUND));
        access.requirePatient(actor, visit.getPatientId());
        return visit;
    }

    private void validate(
            LocalDate date, String hospital, String department, String memo, VisitStatus status) {
        if (date == null
                || status == null
                || (hospital != null && hospital.length() > 200)
                || (department != null && department.length() > 100)
                || (memo != null && memo.length() > 2000))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul"))) && status != VisitStatus.SCHEDULED)
            throw new BusinessException(VisitErrorCode.INVALID_DATE_STATUS);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
