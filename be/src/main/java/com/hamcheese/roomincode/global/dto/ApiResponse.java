package com.hamcheese.roomincode.global.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
    private int statusCode;

    @Builder
    private ApiResponse(String code, String message, T data, int statusCode) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    // 성공 - 메시지만 보낼 때 (기본 200 설정)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .statusCode(200)
                .build();
    }

    // 성공 - 데이터까지 보낼 때
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .data(data)
                .statusCode(200)
                .build();
    }

    // 에러 - 빌더를 직접 쓰지 않고 메서드로 해결
    public static <T> ApiResponse<T> error(String code, String message, int statusCode) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .statusCode(statusCode)
                .build();
    }
}
