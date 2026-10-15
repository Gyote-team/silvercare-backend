package com.gyote.silvercare.visit.api.mapper;

import com.gyote.silvercare.visit.api.dto.response.VisitPageResponseDto;
import com.gyote.silvercare.visit.api.dto.response.VisitResponseDto;
import com.gyote.silvercare.visit.query.model.VisitPageView;
import com.gyote.silvercare.visit.query.model.VisitView;

import org.springframework.stereotype.Component;

/** 방문 조회 모델을 HTTP 응답으로 변환한다. */
@Component
public class VisitResponseMapper {
    /** 상세 및 협업 출력 필드를 응답으로 변환한다. */
    public VisitResponseDto toResponse(VisitView v) {
        return new VisitResponseDto(
                v.visitId(),
                v.patientId(),
                v.visitedOn(),
                v.hospitalName(),
                v.department(),
                v.memo(),
                v.visitStatus(),
                v.documentIds(),
                v.healthRecordIds(),
                v.createdAt(),
                v.updatedAt(),
                v.version());
    }

    /** 목록과 다음 페이지 정보를 변환한다. */
    public VisitPageResponseDto toResponse(VisitPageView page) {
        return new VisitPageResponseDto(
                page.items().stream().map(this::toResponse).toList(),
                page.nextCursor(),
                page.hasNext());
    }
}
