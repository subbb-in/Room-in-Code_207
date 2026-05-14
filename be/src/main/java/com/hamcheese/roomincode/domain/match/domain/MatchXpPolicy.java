package com.hamcheese.roomincode.domain.match.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum MatchXpPolicy {

    // 모드별 XP
    //    모드	EASY(기본점수)	MEDIUM(기본점수)	MEDIUM HARD(기본점수)	HARD(기본점수)	VERY HARD(기본점수)	승리할 경우(기본점수에 추가)	나갔을 때
    //    AI모드	    +6	+7	+8	+9	+10	-	-
    //    친구모드	+12	+14	+16	+18	+20	+40	-50
    //    대회모드	+24	+28	+32	+36	+40	+80	-100

    AI(Map.of(
            "EASY", 6, "MEDIUM", 7, "MEDIUM_HARD", 8, "HARD", 9, "VERY_HARD", 10
    ), 0, 0),

    FRIENDS(Map.of(
            "EASY", 12, "MEDIUM", 14, "MEDIUM_HARD", 16, "HARD", 18, "VERY_HARD", 20
    ), 40, -50),

    RANKING(Map.of( // 기획서의 대회모드
            "EASY", 24, "MEDIUM", 28, "MEDIUM_HARD", 32, "HARD", 36, "VERY_HARD", 40
    ), 80, -100);


    private final Map<String, Integer> scores;
    private final int submitBonus;
    private final int penalty;

    public int getXp(String difficulty) {
        String key = difficulty.toUpperCase().replace(" ", "_");
        return scores.getOrDefault(key, 0);
    }
}
