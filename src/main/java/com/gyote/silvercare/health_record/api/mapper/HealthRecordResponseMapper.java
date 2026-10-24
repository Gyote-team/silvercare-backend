package com.gyote.silvercare.health_record.api.mapper;

import com.gyote.silvercare.health_record.api.dto.response.HealthRecordPageResponseDto;
import com.gyote.silvercare.health_record.api.dto.response.HealthRecordResponseDto;
import com.gyote.silvercare.health_record.query.model.HealthRecordPageView;
import com.gyote.silvercare.health_record.query.model.HealthRecordView;

import org.springframework.stereotype.Component;

/** 건강기록 조회 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class HealthRecordResponseMapper {
    /** 조회 모델을 HTTP 응답으로 변환한다. */
    public HealthRecordResponseDto toResponse(HealthRecordView r) {
        return new HealthRecordResponseDto(
                r.recordId(),
                r.patientId(),
                r.authorUserId(),
                r.authorName(),
                r.visitId(),
                r.body(),
                r.recordedAt(),
                r.createdAt(),
                r.updatedAt(),
                r.proxyWritten());
    }

    /** 커서와 조회 항목을 함께 응답한다. */
    public HealthRecordPageResponseDto toResponse(HealthRecordPageView page) {
        return new HealthRecordPageResponseDto(
                page.items().stream().map(this::toResponse).toList(),
                page.nextCursor(),
                page.hasNext());
    }
}
