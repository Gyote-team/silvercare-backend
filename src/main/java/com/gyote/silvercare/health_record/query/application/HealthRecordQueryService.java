package com.gyote.silvercare.health_record.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.domain.HealthRecord;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.health_record.query.model.HealthRecordView;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HealthRecordQueryService {

    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy accessPolicy;
    private final UserRepository users;
    private final UserQueryService userQueries;

    public HealthRecordQueryService(HealthRecordRepository records, HealthRecordAccessPolicy accessPolicy,
                                    UserRepository users, UserQueryService userQueries) {
        this.records = records;
        this.accessPolicy = accessPolicy;
        this.users = users;
        this.userQueries = userQueries;
    }

    public User requireUser(String kakaoId) {
        return userQueries.requireByKakaoId(kakaoId);
    }

    public java.util.List<HealthRecordView> list(User actor, java.util.UUID patientId, Integer size) {
        int pageSize = size == null ? 20 : size;
        if (pageSize < 1 || pageSize > 50) throw new BusinessException(HealthRecordErrorCode.INVALID_PAGE_SIZE);
        var targetPatientId = accessPolicy.requireAccessiblePatient(actor, patientId);
        var rows = records.findByPatientIdAndDeletedAtIsNullOrderByRecordedAtDescIdDesc(
                targetPatientId, PageRequest.of(0, pageSize));
        Map<java.util.UUID, User> authors = users.findAllById(rows.stream().map(HealthRecord::getAuthorUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return rows.stream().map(record -> new HealthRecordView(
                record.getId(), record.getPatientId(), record.getVisitId(),
                authors.getOrDefault(record.getAuthorUserId(), actor).getName(), record.getAuthorUserId(),
                record.getInputType(), record.getContent(), record.getRecordedAt(), record.isProxyWritten(),
                record.getCreatedAt(), record.getUpdatedAt(), record.getVersion()
        )).toList();
    }
}
