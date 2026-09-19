package com.gyote.silvercare.user.api.controller;

import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.oauth.KakaoLoginSuccessHandler;
import com.gyote.silvercare.global.auth.security.JwtAuthFilter;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.user.api.dto.request.DemoLoginRequest;
import com.gyote.silvercare.user.api.dto.request.RoleRequest;
import com.gyote.silvercare.user.api.dto.response.AuthConfigResponse;
import com.gyote.silvercare.user.api.dto.response.MeResponse;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthApiController {

    private final UserAccountService accounts;
    private final KakaoLoginSuccessHandler loginSuccess;
    private final AuthCookieService authCookies;
    private final String kakaoClientId;
    private final boolean demoLogin;

    public AuthApiController(
            UserAccountService accounts,
            KakaoLoginSuccessHandler loginSuccess,
            AuthCookieService authCookies,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id:unset}") String kakaoClientId,
            @Value("${silvercare.demo-login:false}") boolean demoLogin
    ) {
        this.accounts = accounts;
        this.loginSuccess = loginSuccess;
        this.authCookies = authCookies;
        this.kakaoClientId = kakaoClientId;
        this.demoLogin = demoLogin;
    }

    @GetMapping("/api/auth/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(kakaoReady(), demoLogin);
    }

    @PostMapping("/api/role")
    public ResponseEntity<MeResponse> chooseRole(
            @AuthenticationPrincipal OAuth2User user,
            @RequestBody RoleRequest request,
            HttpServletResponse response
    ) {
        User updated = accounts.chooseRole(kakaoId(user), request.toUserRole());
        refreshSession(user, updated);
        authCookies.write(response, updated);
        return ResponseEntity.ok(toMe(updated));
    }

    @PostMapping("/api/demo/login")
    public ResponseEntity<MeResponse> demoLogin(
            @RequestBody DemoLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (!demoLogin) {
            throw new BusinessException(GlobalErrorCode.DEMO_LOGIN_DISABLED);
        }
        User user = accounts.ensureDemoUser(request.toUserRole());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(JwtAuthFilter.principal(user));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, response);
        authCookies.write(response, user);
        return ResponseEntity.ok(toMe(user));
    }

    @GetMapping("/api/auth/needs-role")
    public Map<String, Boolean> needsRole() {
        return Map.of("needsRole", loginSuccess.needsRole(SecurityContextHolder.getContext().getAuthentication()));
    }

    private MeResponse toMe(User user) {
        return new MeResponse(
                user.getId().toString(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getInviteCode() == null ? null : CareRelationCode.display(user.getInviteCode())
        );
    }

    private void refreshSession(OAuth2User current, User updated) {
        Map<String, Object> attributes = new HashMap<>(current.getAttributes());
        attributes.put("role", updated.getRole().name());
        attributes.put("displayName", updated.getName());
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + updated.getRole().name())),
                attributes,
                "id"
        );
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String registrationId = auth instanceof OAuth2AuthenticationToken oauth
                ? oauth.getAuthorizedClientRegistrationId()
                : "kakao";
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId)
        );
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private boolean kakaoReady() {
        return kakaoClientId != null
                && !kakaoClientId.isBlank()
                && !"unset".equalsIgnoreCase(kakaoClientId);
    }
}
