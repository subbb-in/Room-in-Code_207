package com.hamcheese.roomincode.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthRedisRepository {

    private final StringRedisTemplate redisTemplate;

    // Refresh Token 저장
    public void saveRefreshToken(String email, String refreshToken, long ttl) {
        redisTemplate.opsForValue().set(
                "auth:refresh:" + email,
                refreshToken,
                ttl,
                TimeUnit.MILLISECONDS
        );
    }

    // Refresh Token 조회
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get("auth:refresh:" + email);
    }

    // Refresh Token 삭제
    public void deleteRefreshToken(String email) {
        redisTemplate.delete("auth:refresh:" + email);
    }

    // AccessToken 블랙리스트 등록 - 로그아웃시
    public void blacklistAccessToken(String token, long ttl) {
        redisTemplate.opsForValue().set(
                "auth:blacklist:" + token,
                "logout",
                ttl,
                TimeUnit.MILLISECONDS
        );
    }

    // 블랙리스트인지 체크
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("auth:blacklist:" + token));
    }
}