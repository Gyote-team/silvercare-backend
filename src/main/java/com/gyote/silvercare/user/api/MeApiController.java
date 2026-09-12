package com.gyote.silvercare.user.api;

import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.api.response.MeResponse;
import com.gyote.silvercare.user.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeApiController {

    private final UserQueryService users;

    public MeApiController(UserQueryService users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = users.requireByKakaoId(kakaoId(principal));
        return new MeResponse(
                user.getId().toString(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getInviteCode() == null ? null : CareRelationCode.display(user.getInviteCode())
        );
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
