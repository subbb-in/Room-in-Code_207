package com.hamcheese.roomincode.global.handler;

import com.hamcheese.roomincode.domain.auth.dto.CustomOAuth2User;
import com.hamcheese.roomincode.domain.auth.repository.AuthRedisRepository;
import com.hamcheese.roomincode.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisRepository authRedisRepository;

    @Value("${app.front.base-url}")
    private String frontBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. 로그인 성공한 유저 정보 가져오기
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // Map에서 꺼낼 때 타입 캐스팅 안전하게 처리
        Long userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();

        // 2. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, email);

        log.info("소셜 로그인 성공! userId: {}, email: {}", userId, email);

        // 3. 레디스에 RefreshToken 저장
        authRedisRepository.saveRefreshToken(
                email,
                refreshToken,
                jwtTokenProvider.getRemainTime(refreshToken)
        );

        // 4. 프론트엔드로 리다이렉트 (변수 연결 수정함)
        String targetUrl = UriComponentsBuilder.fromUriString(frontBaseUrl + "/oauth/callback")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}