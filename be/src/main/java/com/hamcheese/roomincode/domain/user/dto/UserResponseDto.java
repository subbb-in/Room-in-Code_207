package com.hamcheese.roomincode.domain.user.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
public class UserResponseDto {
    private Long userId;        //AutoIncrement
    private String email;
    private String nickname;
    private String loginType;
    private LocalDateTime joinedAt;
    private  LocalDateTime updatedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Profile {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String introText;
        private int totalXp;
    }

}
