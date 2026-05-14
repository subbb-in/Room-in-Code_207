package com.hamcheese.roomincode.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingInfoResponseDto {
    private Long rank;
    private Long userId;
    private String nickname;
    private String introText;
    private Integer xp;
    private String profileImageUrl;
}
