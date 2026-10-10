package com.gyote.silvercare.health_record.api.mapper;
import com.gyote.silvercare.health_record.query.model.*;
import com.gyote.silvercare.health_record.api.dto.response.*;
import org.springframework.stereotype.Component;
@Component
public class HealthRecordResponseMapper {
    /** 조회 모델을 HTTP 응답으로 변환한다. */
    public HealthRecordResponseDto toResponse(HealthRecordView r) {
        return new HealthRecordResponseDto(r.recordId(),r.patientId(),r.authorUserId(),r.authorName(),r.visitId(),
                r.body(),r.recordedAt(),r.createdAt(),r.updatedAt(),r.proxyWritten());
    }
    /** 커서와 조회 항목을 함께 응답한다. */
    public HealthRecordPageResponseDto toResponse(HealthRecordPageView page) {
        return new HealthRecordPageResponseDto(page.items().stream().map(this::toResponse).toList(),page.nextCursor(),page.hasNext());
    }
}
