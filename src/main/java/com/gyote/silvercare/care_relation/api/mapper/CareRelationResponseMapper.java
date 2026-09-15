package com.gyote.silvercare.care_relation.api.mapper;

import com.gyote.silvercare.care_relation.api.dto.response.CareRelationResponse;
import com.gyote.silvercare.care_relation.query.model.CareRelationView;
import org.springframework.stereotype.Component;

import java.util.List;

/** Application 조회 모델을 HTTP 응답 DTO로 변환합니다. */
@Component
public class CareRelationResponseMapper {

    public CareRelationResponse toResponse(CareRelationView view) {
        return new CareRelationResponse(
                view.id(),
                view.counterpartName(),
                view.statusLabel(),
                view.status(),
                view.canAccept(),
                view.canReject(),
                view.canCancel(),
                view.canRevoke()
        );
    }

    public List<CareRelationResponse> toResponses(List<CareRelationView> views) {
        return views.stream().map(this::toResponse).toList();
    }
}
