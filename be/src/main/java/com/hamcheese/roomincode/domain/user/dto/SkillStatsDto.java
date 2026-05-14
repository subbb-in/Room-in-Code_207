package com.hamcheese.roomincode.domain.user.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillStatsDto {
    private String skillName;
    private int solvedCount;
}
