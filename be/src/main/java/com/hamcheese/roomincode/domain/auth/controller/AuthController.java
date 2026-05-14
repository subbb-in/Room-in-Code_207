package com.hamcheese.roomincode.domain.auth.controller;

import com.hamcheese.roomincode.domain.auth.dto.AuthRequestDto;
import com.hamcheese.roomincode.domain.auth.dto.AuthResponseDto;
import com.hamcheese.roomincode.domain.auth.service.AuthService;
import com.hamcheese.roomincode.global.dto.ApiResponse;
import com.hamcheese.roomincode.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// 인증 관련 경로는 auth로 따로 분리
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(
            @Valid @RequestBody AuthRequestDto.Login requestDto
    ) {

        AuthResponseDto.TokenInfo tokenInfo = authService.login(requestDto);

        long cookieMaxAge = jwtTokenProvider.getRefreshTokenValidityInSeconds();

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenInfo.getRefreshToken())
                .httpOnly(true)
                .secure(true)         // 로컬 개발시 false, 배포시 true
                .path("/")
                .maxAge(cookieMaxAge)
                .sameSite("None")     // 프론트-백엔드 도메인이 다르면 None, 같으면 Lax/Strict
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그인 성공", tokenInfo.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader
    ) {

        // 헤더 검증
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 유효하지 않습니다.");
        }

        // 헤더에서 토큰 추출
        String accessToken = authHeader.substring(7);
        authService.logout(accessToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // 즉시 만료
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그아웃 성공"));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<String>> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            throw new RuntimeException("Refresh Token이 쿠키에 없습니다.");
        }

        // 재발급된 Access Token만 Body로 반환
        return ResponseEntity.ok(ApiResponse.success("재발급 성공", authService.reissue(refreshToken)));
    }

}
