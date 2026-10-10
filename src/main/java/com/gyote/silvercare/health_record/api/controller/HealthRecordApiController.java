package com.gyote.silvercare.health_record.api.controller;

import com.gyote.silvercare.global.exception.ErrorResponse;
import com.gyote.silvercare.health_record.api.dto.request.HealthRecordWriteRequestDto;
import com.gyote.silvercare.health_record.api.dto.response.HealthRecordPageResponseDto;
import com.gyote.silvercare.health_record.api.dto.response.HealthRecordResponseDto;
import com.gyote.silvercare.health_record.api.mapper.HealthRecordResponseMapper;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.health_record.query.application.HealthRecordQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

/** 건강기록 CRUD 요청과 인증된 계정의 권한 검증을 연결한다. */
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordApiController {
    private final HealthRecordCommandService commands;
    private final HealthRecordQueryService queries;
    private final HealthRecordResponseMapper mapper;
    private final UserQueryService users;

    public HealthRecordApiController(
            HealthRecordCommandService commands,
            HealthRecordQueryService queries,
            HealthRecordResponseMapper mapper,
            UserQueryService users) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
        this.users = users;
    }

    /** 대상 개인의 건강기록 목록을 조회한다. */
    @GetMapping
    public HealthRecordPageResponseDto list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return mapper.toResponse(queries.list(actor(principal), patientId, cursor, size));
    }

    /** 건강기록 상세를 조회한다. */
    @GetMapping("/{recordId}")
    public HealthRecordResponseDto detail(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID recordId) {
        return mapper.toResponse(queries.detail(actor(principal), recordId));
    }

    /** 본인 또는 연결된 개인의 건강기록을 작성한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HealthRecordResponseDto create(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody HealthRecordWriteRequestDto request) {
        User actor = actor(principal);
        var record = commands.create(actor, request.patientId(), request.body(), request.visitId());
        return mapper.toResponse(HealthRecordQueryService.view(record, actor.getName()));
    }

    /** 본인 프로필의 개인 또는 허용된 작성자가 건강기록을 수정한다. */
    @PutMapping("/{recordId}")
    public HealthRecordResponseDto update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID recordId,
            @Valid @RequestBody HealthRecordWriteRequestDto request) {
        User actor = actor(principal);
        var record =
                commands.update(
                        actor, recordId, request.patientId(), request.body(), request.visitId());
        return mapper.toResponse(queries.detail(actor, record.getId()));
    }

    /** 본인 프로필의 개인 또는 허용된 작성자가 기록을 소프트 삭제한다. */
    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID recordId) {
        commands.delete(actor(principal), recordId);
    }

    private User actor(OAuth2User principal) {
        Object kakaoId = principal.getAttribute("id");
        return users.requireByKakaoId(String.valueOf(kakaoId));
    }

    /** 잘못된 UUID와 페이지 크기를 공통 오류 계약으로 반환한다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidParameter(
            MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HealthRecordErrorCode.INVALID_REQUEST));
    }

    /** 겹친 수정·삭제는 재조회 후 다시 시도하도록 안내한다. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> concurrentUpdate(
            ObjectOptimisticLockingFailureException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HealthRecordErrorCode.CONFLICT));
    }
}
