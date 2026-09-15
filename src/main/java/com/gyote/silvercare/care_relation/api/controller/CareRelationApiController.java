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

    @GetMapping("/api/care-relations")
    public List<CareRelationResponse> list(@AuthenticationPrincipal OAuth2User user) {
        return responseMapper.toResponses(queries.listFor(queries.requireUser(kakaoId(user))));
    }

    @PostMapping("/api/care-relations")
    public ResponseEntity<List<CareRelationResponse>> request(
            @AuthenticationPrincipal OAuth2User user,
            @Valid @RequestBody InviteCodeRequest body
    ) {
        User me = queries.requireUser(kakaoId(user));
        commands.request(me, body.getInviteCode());
        return ResponseEntity.ok(responseMapper.toResponses(queries.listFor(me)));
    }

    @PostMapping("/api/care-relations/{id}/accept")
    public ResponseEntity<?> accept(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.accept(queries.requireUser(kakaoId(user)), id));
    }

    @PostMapping("/api/care-relations/{id}/reject")
    public ResponseEntity<?> reject(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.reject(queries.requireUser(kakaoId(user)), id));
    }

    @PostMapping("/api/care-relations/{id}/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal OAuth2User user, @PathVariable UUID id) {
        return mutate(user, () -> commands.cancel(queries.requireUser(kakaoId(user)), id));
    }

    @PostMapping("/api/care-relations/{id}/revoke")
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
