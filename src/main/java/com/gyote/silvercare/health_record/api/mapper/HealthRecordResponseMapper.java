package com.gyote.silvercare.health_record.api.mapper;

import com.gyote.silvercare.health_record.api.dto.response.HealthRecordResponse;
import com.gyote.silvercare.health_record.domain.HealthRecord;
import com.gyote.silvercare.health_record.query.model.HealthRecordView;
import org.springframework.stereotype.Component;

@Component
public class HealthRecordResponseMapper {

    public HealthRecordResponse toResponse(HealthRecord record, String authorName) {
        return new HealthRecordResponse(record.getId(), record.getPatientId(), record.getVisitId(), authorName,
                record.getAuthorUserId(), record.getInputType(), record.getContent(), record.getRecordedAt(),
                record.isProxyWritten(), record.getCreatedAt(), record.getUpdatedAt(), record.getVersion());
    }

    public HealthRecordResponse toResponse(HealthRecordView view) {
        return new HealthRecordResponse(view.id(), view.patientId(), view.visitId(), view.authorName(),
                view.authorUserId(), view.inputType(), view.content(), view.recordedAt(), view.proxyWritten(),
                view.createdAt(), view.updatedAt(), view.version());
    }
}
