package com.hamcheese.roomincode.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AuthResponseDto {

    // 로그인 성공 시 토큰 전달용
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
    }
}
