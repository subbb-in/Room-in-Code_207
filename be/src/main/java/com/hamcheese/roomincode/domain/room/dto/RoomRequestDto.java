package com.hamcheese.roomincode.domain.room.dto;

import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.room.type.Difficulty;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
public class RoomRequestDto {

    /**
     * FRIENDS 모드 로직
     */

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Join {
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoomCreateRequest {

        @NotBlank(message = "방 제목은 필수입니다.")
        private String title;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;

        @Min(2) @Max(4)
        private int capacity; // 인원 수

        @NotNull(message = "언어는 필수입니다.")
        private MatchLanguage language;

        @NotBlank(message = "알고리즘 선택은 필수입니다.")
        private String algorithm;

        @NotNull(message = "난이도는 필수입니다.")
        private Difficulty difficulty;

        @Positive(message = "턴 시간은 양수여야 합니다.")
        private int turnTime;

        @NotBlank
        private String mode;
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class List {
        private MatchLanguage language; // 언어로 바꿈
        private String difficulty; // 난이도
        private String algorithm; // 알고리즘 유형
        private String mode; // 모드 -> 여기서는 친구 모드로 고정
    }

    /**
     * AI 모드 로직
     */

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AiRoomCreateRequest {

        @NotNull(message = "난이도는 필수입니다.")
        private Difficulty difficulty;

        @NotNull(message = "언어는 필수입니다.")
        private MatchLanguage language;

        @NotBlank(message = "알고리즘 선택은 필수입니다.")
        private String algorithm;

        @Positive(message = "턴 시간은 양수여야 합니다.")
        @Min(value = 40, message = "턴 시간은 최소 40초입니다.")
        @Max(value = 80, message = "턴 시간은 최대 80초입니다.")
        private int turnTime;
    }
}