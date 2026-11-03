package com.gyote.silvercare.previsit_summary.api.mapper;

import com.gyote.silvercare.previsit_summary.api.dto.response.PrevisitSummaryResponseDto;
import com.gyote.silvercare.previsit_summary.query.model.PrevisitSummaryView;
import org.springframework.stereotype.Component;

/** Query 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class PrevisitSummaryResponseMapper {
    /** 작업 상태만 변환한다. */
    public PrevisitSummaryResponseDto.Operation operation(PrevisitSummaryView.Operation v) {
        return new PrevisitSummaryResponseDto.Operation(v.requestId(),v.operationType(),v.jobStatus(),
                v.resultStatus(),v.summaryId(),v.retryable(),v.errorCode());
    }
    /** 현재 유효한 원본과 요약을 변환한다. */
    public PrevisitSummaryResponseDto.Detail detail(PrevisitSummaryView.Detail v) {
        return new PrevisitSummaryResponseDto.Detail(v.summaryId(),v.summary(),v.usedHealthRecordIds(),
                v.windowDays(),v.createdAt(),v.sourceRecords().stream().map(s ->
                new PrevisitSummaryResponseDto.Source(s.healthRecordId(),s.recordedAt(),s.content())).toList());
    }
}
