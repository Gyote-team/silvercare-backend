package com.gyote.silvercare.user.api.controller;

import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.api.dto.response.MeResponse;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeApiController {

    private final UserQueryService users;
    private final PatientRepository patients;

    public MeApiController(UserQueryService users, PatientRepository patients) {
        this.users = users;
        this.patients = patients;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = users.requireByKakaoId(kakaoId(principal));
        return new MeResponse(
                user.getId().toString(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name(),
                inviteCodeFor(user)
        );
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private String inviteCodeFor(User user) {
        if (user.getRole() != UserRole.PATIENT) {
            return null;
        }
        return patients.findByUserId(user.getId())
                .map(Patient::getInviteCode)
                .map(CareRelationCode::display)
                .orElse(null);
    }
}
