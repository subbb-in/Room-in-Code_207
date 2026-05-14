package com.hamcheese.roomincode.domain.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {
    private Long userId;
    private String email;
    private String password; // 암호화된 값
    private String nickname;
    private String loginType;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}
