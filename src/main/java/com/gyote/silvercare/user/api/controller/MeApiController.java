package com.gyote.silvercare.user.api.controller;

import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.user.api.dto.request.WithdrawalRequest;
import com.gyote.silvercare.user.api.dto.response.WithdrawalResponse;
import com.gyote.silvercare.user.command.application.UserWithdrawalService;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.api.dto.response.MeResponse;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeApiController {

    private final UserQueryService users;
    private final PatientRepository patients;
    private final UserWithdrawalService withdrawals;
    private final AuthCookieService authCookies;
    private final UserAccountService accounts;

    public MeApiController(
            UserQueryService users,
            PatientRepository patients,
            UserWithdrawalService withdrawals,
            AuthCookieService authCookies,
            UserAccountService accounts
    ) {
        this.users = users;
        this.patients = patients;
        this.withdrawals = withdrawals;
        this.authCookies = authCookies;
        this.accounts = accounts;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = users.requireByKakaoId(kakaoId(principal));
        accounts.ensurePatientProfile(user);
        return responseFor(user);
    }

    @DeleteMapping("/api/me")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody WithdrawalRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        WithdrawalResponse result = withdrawals.withdraw(kakaoId(principal), request.getConfirmed());
        authCookies.clearAll(response);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(result);
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private MeResponse responseFor(User user) {
        Patient patient = patients.findByUserId(user.getId()).orElse(null);
        String inviteCode = patient == null ? null : CareRelationCode.display(patient.getInviteCode());
        return MeResponse.of(user, inviteCode, patient == null ? null : patient.getId().toString());
    }
}
