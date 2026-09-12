package com.gyote.silvercare.global.config;

import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.oauth.KakaoLoginSuccessHandler;
import com.gyote.silvercare.global.auth.oauth.KakaoOAuth2UserService;
import com.gyote.silvercare.global.auth.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final KakaoOAuth2UserService kakaoOAuth2UserService;
    private final KakaoLoginSuccessHandler kakaoLoginSuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;
    private final AuthCookieService authCookies;
    private final FrontendOrigin frontend;

    public SecurityConfig(
            KakaoOAuth2UserService kakaoOAuth2UserService,
            KakaoLoginSuccessHandler kakaoLoginSuccessHandler,
            JwtAuthFilter jwtAuthFilter,
            AuthCookieService authCookies,
            FrontendOrigin frontend
    ) {
        this.kakaoOAuth2UserService = kakaoOAuth2UserService;
        this.kakaoLoginSuccessHandler = kakaoLoginSuccessHandler;
        this.jwtAuthFilter = jwtAuthFilter;
        this.authCookies = authCookies;
        this.frontend = frontend;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clients
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/login", "/home", "/role", "/terms", "/privacy",
                                "/api/auth/config", "/api/demo/login", "/logout",
                                "/oauth2/**", "/login/oauth2/**", "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/care-relations").hasRole("CAREGIVER")
                        .requestMatchers(HttpMethod.POST, "/api/care-relations/*/accept").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.POST, "/api/care-relations/*/reject").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.POST, "/api/care-relations/*/cancel").hasRole("CAREGIVER")
                        .requestMatchers(HttpMethod.POST, "/api/care-relations/*/revoke").hasAnyRole("PATIENT", "CAREGIVER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) -> {
                    String uri = request.getRequestURI();
                    if (uri != null && uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"unauthorized\"}");
                        return;
                    }
                    response.sendRedirect(frontend.path("/login"));
                }))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth -> oauth
                        .loginPage(frontend.path("/login"))
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(kakaoAuthorizationRequestResolver(clients)))
                        .successHandler(kakaoLoginSuccessHandler)
                        .userInfoEndpoint(user -> user.userService(kakaoOAuth2UserService))
                        .failureHandler((request, response, exception) -> {
                            log.warn("카카오 로그인 실패: {}", exception.getMessage(), exception);
                            response.sendRedirect(frontend.path("/login?error=" + loginError(exception)));
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String accept = request.getHeader("Accept");
                            if (accept != null && accept.contains("application/json")) {
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                                return;
                            }
                            response.sendRedirect(frontend.path("/login?logout"));
                        })
                        .deleteCookies("SILVERCARE_SESSION", "SILVERCARE_TOKEN")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .addLogoutHandler((request, response, authentication) -> authCookies.clear(response))
                );
        return http.build();
    }

    private static OAuth2AuthorizationRequestResolver kakaoAuthorizationRequestResolver(
            ClientRegistrationRepository clients
    ) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> params.put("prompt", "login")));
        return resolver;
    }

    static String loginError(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof OAuth2AuthenticationException oauth
                    && "access_denied".equals(oauth.getError().getErrorCode())) {
                return "canceled";
            }
            current = current.getCause();
        }
        return "fail";
    }
}
