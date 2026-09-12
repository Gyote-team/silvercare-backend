package com.gyote.silvercare.care_relation.api;

import com.gyote.silvercare.care_relation.api.request.InviteCodeRequest;
import com.gyote.silvercare.care_relation.api.response.CareRelationResponse;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.query.application.CareRelationQueryService;
import com.gyote.silvercare.global.exception.ApiError;
import com.gyote.silvercare.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CareRelationApiController {

    private final CareRelationCommandService commands;
    private final CareRelationQueryService queries;

    public CareRelationApiController(CareRelationCommandService commands, CareRelationQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/api/care-relations")
    public List<CareRelationResponse> list(@AuthenticationPrincipal OAuth2User user) {
        return queries.listFor(queries.requireUser(kakaoId(user)));
    }

    @PostMapping("/api/care-relations")
    public ResponseEntity<?> request(
            @AuthenticationPrincipal OAuth2User user,
            @RequestBody InviteCodeRequest body
    ) {
        try {
            User me = queries.requireUser(kakaoId(user));
            commands.request(me, body.getInviteCode());
            return ResponseEntity.ok(queries.listFor(me));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiError.of(ex.getMessage()));
        }
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

    private ResponseEntity<?> mutate(OAuth2User user, Runnable action) {
        try {
            action.run();
            return ResponseEntity.ok(queries.listFor(queries.requireUser(kakaoId(user))));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiError.of(ex.getMessage()));
        }
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
