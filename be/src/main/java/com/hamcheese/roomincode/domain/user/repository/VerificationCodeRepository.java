package com.hamcheese.roomincode.domain.user.repository;

/**
 * 이메일 인증 코드를 위한 repository
 * <p>이메일 인증코드를 저장하기 위한 인터페이스</p>
 */
public interface VerificationCodeRepository {
    void saveCode(String email, String code);
    String getCode(String email);
    void markVerified(String email);
    boolean isVerified(String email);
    void removeCode(String email);
}
