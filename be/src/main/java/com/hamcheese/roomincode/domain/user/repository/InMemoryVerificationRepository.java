package com.hamcheese.roomincode.domain.user.repository;

import com.hamcheese.roomincode.global.config.EmailConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Primary
@RequiredArgsConstructor
/**
 * RedisVerificationRepository를 만들 시, @Primary 제거할 것
 */
public class InMemoryVerificationRepository implements VerificationCodeRepository {
    //메모리 저장소 (Key: 이메일, Value: 인증정보객체)
    private final Map<String, VerificationSession> storage = new ConcurrentHashMap<>();

    private final EmailConfig emailConfig;

    //데이터 담을 내부 클래스
    @Getter
    @AllArgsConstructor
    static class VerificationSession {
        private String code;
        private LocalDateTime expireTime;
        private boolean verified;

        //인증 상태 변경 (Setter 안쓰고 이 메서드 씀)
        public void verify() {
            this.verified = true;
        }
    }

    @Override
    public void saveCode(String email, String code) {
        //EXPIRATION_TIME분 뒤 만료
        storage.put(email, new VerificationSession(code, LocalDateTime.now().plusMinutes(emailConfig.getExpirationMinutes()), false));
    }

    @Override
    public String getCode(String email) {
        VerificationSession session = storage.get(email);

        // 세션 없거나 시간 만료시 null
        if (session == null || session.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return session.getCode();
    }

    @Override
    public void markVerified(String email) {
        VerificationSession session = storage.get(email);
        if (session != null) {
            session.verify();
        }
    }

    @Override
    public boolean isVerified(String email) {
        VerificationSession session = storage.get(email);
        // 데이터 존재 && 만료 안됨 && 인증 완료됨
        return session != null
                && session.getExpireTime().isAfter(LocalDateTime.now())
                && session.isVerified();
    }

    @Override
    public void removeCode(String email) {
        storage.remove(email);
    }
}
