package com.gyote.silvercare.care_relation.api.controller;

import com.gyote.silvercare.care_relation.api.dto.request.InviteCodeRequest;
import com.gyote.silvercare.care_relation.api.dto.response.CareRelationResponse;
import com.gyote.silvercare.care_relation.api.mapper.CareRelationResponseMapper;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.query.application.CareRelationQueryService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
public class CareRelationApiController {

    private final CareRelationCommandService commands;
    private final CareRelationQueryService queries;
    private final CareRelationResponseMapper responseMapper;

    public CareRelationApiController(
            CareRelationCommandService commands,
            CareRelationQueryService queries,
            CareRelationResponseMapper responseMapper
    ) {
        this.commands = commands;
        this.queries = queries;
        this.responseMapper = responseMapper;
    }

    /** 현재 사용자와 관련된 개인·보호자 연결 목록을 반환한다. */
    @GetMapping("/api/care-relations")
    public List<CareRelationResponse> list(@AuthenticationPrincipal OAuth2User user) {
        return responseMapper.toResponses(queries.listFor(queries.requireUser(kakaoId(user))));
    }

    /** 보호자가 개인의 초대 코드로 새 연결 요청을 만든다. */
    @PostMapping("/api/care-relations")
    public ResponseEntity<List<CareRelationResponse>> request(
            @AuthenticationPrincipal OAuth2User user,
            @Valid @RequestBody InviteCodeRequest body
    ) {
        User me = queries.requireUser(kakaoId(user));
        commands.request(me, body.getInviteCode());
        return ResponseEntity.ok(responseMapper.toResponses(queries.listFor(me)));
    }

    /** 현재 사용자가 당사자인 연결 상세를 반환한다. */
    @GetMapping("/api/care-relations/{id}")
    public CareRelationResponse detail(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return responseMapper.toResponse(queries.detailFor(queries.requireUser(kakaoId(user)), id));
    }

    /** 개인이 대기 중인 연결 요청을 수락한다. */
    @PostMapping("/api/care-relations/{id}/accept")
    public ResponseEntity<?> accept(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.accept(queries.requireUser(kakaoId(user)), id));
    }

    /** 개인이 대기 중인 연결 요청을 거절한다. */
    @PostMapping("/api/care-relations/{id}/reject")
    public ResponseEntity<?> reject(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.reject(queries.requireUser(kakaoId(user)), id));
    }

    /** 요청을 만든 보호자가 대기 중인 연결 요청을 취소한다. */
    @DeleteMapping("/api/care-relations/{id}/request")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.cancel(queries.requireUser(kakaoId(user)), id));
    }

    /** 연결 당사자가 활성 연결을 해제한다. */
    @DeleteMapping("/api/care-relations/{id}")
    public ResponseEntity<?> revoke(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.revoke(queries.requireUser(kakaoId(user)), id));
    }

    private ResponseEntity<List<CareRelationResponse>> mutate(OAuth2User user, Runnable action) {
        action.run();
        return ResponseEntity.ok(responseMapper.toResponses(queries.listFor(queries.requireUser(kakaoId(user)))));
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
