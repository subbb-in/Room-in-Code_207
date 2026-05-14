package com.hamcheese.roomincode.domain.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmojiDto {

    private String type; // "EMOJI", "CODE_UPDATE" 등 분기 처리를 위해 type 구분
    private String matchId;
    private String userId;
    private String emoji;
}
