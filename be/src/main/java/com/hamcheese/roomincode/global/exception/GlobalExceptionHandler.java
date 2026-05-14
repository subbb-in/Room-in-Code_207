package com.hamcheese.roomincode.global.exception;

import com.hamcheese.roomincode.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [데이터 충돌: 409 Conflict - DataIntegrityViolation]
     * DB의 Unique Constraint(고유 제약 조건)을 위반했을 때 발생
     */
    // 데이터 제약 조건 위반(DataIntegrityViolationException)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Database integrity violation (Possible Conflict 409): {}", ex.getMessage());

        String detailedMessage = "요청된 데이터가 이미 존재하거나 유효성 제약 조건을 위반했습니다.";

        ApiResponse<?> response = ApiResponse.error(
                "CONFLICT", // 에러코드
                detailedMessage, // 메세지
                HttpStatus.CONFLICT.value()); // 409

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // 잘못된 파일 형식(IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleArgumentException(IllegalArgumentException exc) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // HTTP 400
                .body(ApiResponse.error(
                        "BAD_REQUEST",                 // 에러 코드 (프론트 식별용)
                        exc.getMessage(),               // 예외에 담긴 메시지 그대로 전달
                        HttpStatus.BAD_REQUEST.value() // 400
                ));
    }

    /**
     * [데이터 없음: 404 Not Found]
     * Service에서 throw new ResourceNotFoundException(...) 할 때
     */
    // 데이터 없음(ResourceNotFoundException)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        // errorCode는 로그로만 남기고, 클라이언트에는 메시지만 전달
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(response);
    }

    // 인증 실패(BadCredentialsException 등)
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(
                        "UNAUTHORIZED",
                        "이메일 또는 비밀번호가 일치하지 않습니다.",
                        HttpStatus.UNAUTHORIZED.value()
                ));
    }

    // 그 외 모든 예외(위에서 거르지 못한 오류들)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exc) {
        exc.printStackTrace(); // 서버 로그는 남기고
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다.",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
