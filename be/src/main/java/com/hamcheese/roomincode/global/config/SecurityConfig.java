package com.hamcheese.roomincode.global.config;

import com.hamcheese.roomincode.domain.auth.service.CustomOAuth2UserService;
import com.hamcheese.roomincode.global.handler.OAuth2SuccessHandler;
import com.hamcheese.roomincode.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Value("${app.front.base-url}")
    private String frontBaseUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 등록
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/matches/**").permitAll() //TODO: 지워
                        .requestMatchers("/api/sessions/**").permitAll()
                        .requestMatchers("/images/**").permitAll()              // 이미지 경로 접근 누구나 가능하게
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/api/users/password/forgot").permitAll()          // 비밀번호 찾기 할 땐, 굳이 accessToken 필요 없음
                        .requestMatchers("/api/users/password/reset/**").permitAll()          // 비밀번호 찾기 할 땐, 굳이 accessToken 필요 없음
                        .requestMatchers("/v3/api-docs/**").permitAll() //TODO: 나중에 해제할 것
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api/users/email-verification/**").permitAll()
                        .requestMatchers("/api/rooms/**").authenticated()
                        .requestMatchers("/api/matches/callback").permitAll() // Judge0 콜백 허용
                        // WebSocket 테스트를 위한 임시 설정 (TODO: 테스트 완료 후 인증 추가 + 나중에 해제할 것)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/rooms/**").permitAll() //TODO: 지워
                        .requestMatchers("/api/matches/**").permitAll() //TODO: 테스트 후 지워
                       .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 소셜로그인 로직
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // 아까 만든 Service 등록
                        )
                        .successHandler(oAuth2SuccessHandler)     // 아까 만든 Handler 등록
                );
        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(frontBaseUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
