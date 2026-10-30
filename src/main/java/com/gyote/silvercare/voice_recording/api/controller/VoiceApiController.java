package com.gyote.silvercare.voice_recording.api.controller;

import com.gyote.silvercare.global.exception.*;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.voice_recording.api.dto.request.VoiceConfirmRequestDto;
import com.gyote.silvercare.voice_recording.api.dto.response.VoiceTranscriptionResponseDto;
import com.gyote.silvercare.voice_recording.api.mapper.VoiceResponseMapper;
import com.gyote.silvercare.voice_recording.command.application.VoiceCommandService;
import com.gyote.silvercare.voice_recording.error.VoiceRecordingErrorCode;
import com.gyote.silvercare.voice_recording.query.application.VoiceQueryService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/** 건강 음성과 방문 녹음의 공통 업로드·상태·확정 진입점. */
@RestController
public class VoiceApiController {
    private final VoiceCommandService commands;
    private final VoiceQueryService queries;
    private final VoiceResponseMapper mapper;
    private final UserQueryService users;

    public VoiceApiController(
            VoiceCommandService commands,
            VoiceQueryService queries,
            VoiceResponseMapper mapper,
            UserQueryService users) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
        this.users = users;
    }

    /** 건강 음성을 업로드하고 비동기 작업 위치를 반환한다. */
    @PostMapping(
            value = "/api/health-records/voice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceTranscriptionResponseDto> uploadHealth(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(defaultValue = "false") boolean consent,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return upload(actor(principal), audio, patientId, visitId, false, consent, key);
    }

    /** 방문 녹음을 업로드하고 고지 확인을 함께 기록한다. */
    @PostMapping(
            value = "/api/visits/{visitId}/recordings",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceTranscriptionResponseDto> uploadVisit(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID visitId,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(defaultValue = "false") boolean consent,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return upload(actor(principal), audio, patientId, visitId, true, consent, key);
    }

    /** 음성 작업의 현재 상태와 초안을 반환한다. */
    @GetMapping("/api/transcriptions/{id}")
    public VoiceTranscriptionResponseDto detail(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        return mapper.toResponse(queries.detail(actor(principal), id));
    }

    /** 대상 건강 음성의 최근 작업 목록을 반환한다. */
    @GetMapping("/api/transcriptions")
    public List<VoiceTranscriptionResponseDto> list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId) {
        return queries.list(actor(principal), patientId, null).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** 방문에 연결된 최근 녹음 작업을 반환한다. */
    @GetMapping("/api/visits/{visitId}/recordings")
    public List<VoiceTranscriptionResponseDto> listVisit(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID visitId) {
        return queries.list(actor(principal), null, visitId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** 확인한 초안만 최종 건강·진료기록으로 한 번 확정한다. */
    @PostMapping("/api/transcriptions/{id}/confirm")
    public VoiceTranscriptionResponseDto confirm(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @Valid @RequestBody VoiceConfirmRequestDto body) {
        User actor = actor(principal);
        return mapper.toResponse(
                queries.detail(actor, commands.confirm(actor, id, body.body(), body.version())));
    }

    /** 확정 전 작업을 취소한다. */
    @DeleteMapping("/api/transcriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        commands.cancel(actor(principal), id);
    }

    /** 실패한 작업의 원본으로 재처리를 요청한다. */
    @PostMapping("/api/transcriptions/{id}/retry")
    public VoiceTranscriptionResponseDto retry(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        User actor = actor(principal);
        return mapper.toResponse(queries.detail(actor, commands.retry(actor, id)));
    }

    private ResponseEntity<VoiceTranscriptionResponseDto> upload(
            User actor,
            MultipartFile audio,
            UUID patient,
            UUID visit,
            boolean consultation,
            boolean consent,
            String key) {
        if (audio.getSize() > 20 * 1024 * 1024)
            throw new BusinessException(VoiceRecordingErrorCode.TOO_LARGE);
        try {
            UUID id =
                    commands.upload(
                            actor, patient, visit, consultation, consent, key, audio.getBytes());
            return ResponseEntity.accepted()
                    .location(java.net.URI.create("/api/transcriptions/" + id))
                    .body(mapper.toResponse(queries.detail(actor, id)));
        } catch (IOException e) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    private User actor(OAuth2User principal) {
        Object id = principal.getAttribute("id");
        return users.requireByKakaoId(String.valueOf(id));
    }
}
