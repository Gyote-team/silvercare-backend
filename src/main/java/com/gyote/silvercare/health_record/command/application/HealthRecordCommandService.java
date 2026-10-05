package com.gyote.silvercare.health_record.command.application;

import com.gyote.silvercare.health_record.domain.HealthRecord;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 건강 기록 작성은 대상 개인과 작성자를 함께 고정해, 보호자 기록이 다른 개인에게 섞이지 않게 한다. */
@Service
public class HealthRecordCommandService {

    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy accessPolicy;
    private final UserRepository users;

    public HealthRecordCommandService(HealthRecordRepository records, HealthRecordAccessPolicy accessPolicy,
                                      UserRepository users) {
        this.records = records;
        this.accessPolicy = accessPolicy;
        this.users = users;
    }

    @Transactional
    public HealthRecord create(User actor, HealthRecordCommand command) {
        var patientId = accessPolicy.requireAccessiblePatient(actor, command.patientId());
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            // 같은 작성자의 재시도는 사용자 행을 직렬화해, 동시에 들어온 요청도 한 건만 만든다.
            users.findAllByIdForUpdate(java.util.List.of(actor.getId()));
            var existing = records.findByPatientIdAndAuthorUserIdAndIdempotencyKey(
                    patientId, actor.getId(), command.idempotencyKey());
            if (existing.isPresent()) return existing.get();
        }
        return records.save(HealthRecord.create(
                patientId,
                actor.getId(),
                command.visitId(),
                command.inputType(),
                command.content().trim(),
                command.recordedAt() == null ? Instant.now() : command.recordedAt(),
                !patientId.equals(accessPolicy.requireAccessiblePatient(actor, null)),
                blankToNull(command.idempotencyKey())
        ));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
