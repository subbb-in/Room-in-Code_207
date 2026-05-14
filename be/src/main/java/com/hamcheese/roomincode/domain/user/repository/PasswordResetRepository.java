package com.hamcheese.roomincode.domain.user.repository;

// 비밀번호 재설정을 위한 repository 관련 인터페이스
public interface PasswordResetRepository {

    void saveToken(String token, Long userId);
    String getUserId(String token);
    void deleteToken(String token);
    boolean existsToken(String token);

}
