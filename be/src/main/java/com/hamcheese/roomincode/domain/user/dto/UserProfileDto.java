package com.hamcheese.roomincode.domain.user.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long userId;
    private String loginType; // 로그인 타입 SOCIAL / LOCAL
    private String nickname;
    private String profileImageUrl;
    private String introText;
    private int totalXp;
    private String email;
    private java.sql.Timestamp joinedAt;
    // 통계 데이터 리스트
    private List<SkillStatsDto> skillStats;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String nickname;
        private String profileImageUrl;
        private String introText;
    }

}