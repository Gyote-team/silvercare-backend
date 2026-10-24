package com.gyote.silvercare.care_relation.api.controller;

import com.gyote.silvercare.care_relation.api.mapper.CareRelationResponseMapper;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.query.application.CareRelationQueryService;
import com.gyote.silvercare.care_relation.query.model.CareRelationView;
import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.global.auth.oauth.KakaoLoginSuccessHandler;
import com.gyote.silvercare.global.auth.oauth.KakaoOAuth2UserService;
import com.gyote.silvercare.global.auth.security.JwtAuthFilter;
import com.gyote.silvercare.global.config.FrontendOrigin;
import com.gyote.silvercare.global.config.SecurityConfig;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 개인·보호자 연결 조회 HTTP 응답의 JSON 계약을 검증한다. */
@WebMvcTest(CareRelationApiController.class)
@Import({CareRelationResponseMapper.class, SecurityConfig.class, FrontendOrigin.class, JwtAuthFilter.class})
class CareRelationApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CareRelationCommandService commands;

    @MockBean
    private CareRelationQueryService queries;

    @MockBean
    private JwtService jwt;

    @MockBean
    private AuthCookieService cookies;

    @MockBean
    private UserQueryService userQueries;

    @MockBean
    private KakaoOAuth2UserService kakaoOAuth2UserService;

    @MockBean
    private KakaoLoginSuccessHandler kakaoLoginSuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrations;

    @Test
    void detailIncludesCaregiverUserIdAndCounterpartRoleInJson() throws Exception {
        UUID relationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID caregiverId = UUID.randomUUID();
        User caregiver = mock(User.class);
        given(caregiver.getId()).willReturn(caregiverId);
        given(caregiver.getStatus()).willReturn(UserStatus.ACTIVE);
        CareRelationView view = new CareRelationView(
                relationId,
                patientId,
                caregiverId,
                "김순자",
                UserRole.PATIENT,
                "연결됨",
                CareRelationStatus.ACTIVE,
                false,
                false,
                false,
                true,
                Instant.parse("2026-10-04T00:00:00Z"),
                Instant.parse("2026-10-04T00:10:00Z"),
                null
        );
        given(queries.requireUser("kakao-minji")).willReturn(caregiver);
        given(queries.detailFor(caregiver, relationId)).willReturn(view);
        given(userQueries.findByKakaoId("kakao-minji")).willReturn(Optional.of(caregiver));

        mockMvc.perform(get("/api/care-relations/{id}", relationId)
                        .with(oauth2Login().oauth2User(oauthUser("kakao-minji", caregiverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.caregiverUserId").value(caregiverId.toString()))
                .andExpect(jsonPath("$.counterpartName").value("김순자"))
                .andExpect(jsonPath("$.counterpartRole").value("PATIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private OAuth2User oauthUser(String kakaoId, UUID userId) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_CAREGIVER")),
                Map.of("id", kakaoId, "userId", userId.toString()),
                "id"
        );
    }
}
