package com.hamcheese.roomincode.domain.match.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HintResponseDto {

    private String type; // "HINT_RESPONSE"
    private String matchId;
    private String userId;
    private int sequence; // 1: 자료구조, 2: 알고리즘, 3: 엣지케이스
    private String content;
    private int remainingHp;
}
