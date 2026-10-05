package com.gyote.silvercare.health_record.api.controller;

import com.gyote.silvercare.health_record.api.dto.request.HealthRecordCreateRequest;
import com.gyote.silvercare.health_record.api.dto.response.HealthRecordResponse;
import com.gyote.silvercare.health_record.api.mapper.HealthRecordResponseMapper;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommand;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.health_record.query.application.HealthRecordQueryService;
import com.gyote.silvercare.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 현재 선택한 patientId 단위로 건강 기록을 조회·작성하는 API입니다. */
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordApiController {

    private final HealthRecordCommandService commands;
    private final HealthRecordQueryService queries;
    private final HealthRecordResponseMapper responseMapper;

    public HealthRecordApiController(HealthRecordCommandService commands, HealthRecordQueryService queries,
                                     HealthRecordResponseMapper responseMapper) {
        this.commands = commands;
        this.queries = queries;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public List<HealthRecordResponse> list(@AuthenticationPrincipal OAuth2User user,
                                            @RequestParam(required = false) UUID patientId,
                                            @RequestParam(required = false) Integer size) {
        User actor = queries.requireUser(kakaoId(user));
        return queries.list(actor, patientId, size).stream().map(responseMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HealthRecordResponse create(@AuthenticationPrincipal OAuth2User user,
                                       @Valid @RequestBody HealthRecordCreateRequest request) {
        User actor = queries.requireUser(kakaoId(user));
        var record = commands.create(actor, new HealthRecordCommand(
                request.patientId(), request.visitId(), request.inputType(), request.content(),
                request.recordedAt(), request.idempotencyKey()));
        return responseMapper.toResponse(record, actor.getName());
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
