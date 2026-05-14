package com.hamcheese.roomincode.domain.user.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Repository
@Primary
@RequiredArgsConstructor
public class InMemoryPasswordResetRepository implements PasswordResetRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // application.properties에서 시간 설정을 가져옵니다.
    @Value("${app.auth.token-validity-minutes}")
    private long tokenValidityMinutes;

    // Redis Key 접두사 (비밀번호 재설정 전용)
    private static final String PREFIX = "auth:pwd:reset:";

    // UUID 토큰과 유저 ID 저장
    @Override
    public void saveToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                PREFIX + token,
                String.valueOf(userId), // Redis에는 String으로 저장
                Duration.ofMinutes(tokenValidityMinutes)
        );
    }

    // 토큰으로 유저 ID 조회
    @Override
    public String getUserId(String token) {
        return redisTemplate.opsForValue().get(PREFIX + token);
    }

    // 토큰 삭제 (사용 후 또는 만료 시)
    @Override
    public void deleteToken(String token) {
        redisTemplate.delete(PREFIX + token);
    }

    // 토큰 존재 여부 확인
    @Override
    public boolean existsToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
