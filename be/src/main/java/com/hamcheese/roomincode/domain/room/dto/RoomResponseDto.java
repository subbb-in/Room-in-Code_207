package com.hamcheese.roomincode.domain.room.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import lombok.*;

public class RoomResponseDto {

    @Getter
    @Builder
    public static class RoomCreateResponse {
        private String roomId;
        private String title;
        private MatchLanguage language;
        private String algorithm;
        private String difficulty;
        private int capacity;
        private int count;
        private String state;
        private String mode;
    }

    @Getter
    @Builder
    public static class RoomLeaveResponse {
        private String message;
        private boolean isDeleted; // 방 폭파 여부(true-폭파)
        private Long remainingCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        @JsonProperty("id")
        private String roomId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("pwd")
        private String password;

        @JsonProperty("cnt")
        private int count;

        @JsonProperty("max")
        private int capacity;

        @JsonProperty("lang")
        private MatchLanguage language;

        @JsonProperty("diff")
        private String difficulty;

        @JsonProperty("algo")
        private String algorithm;

        @JsonProperty("time")
        private String turnTime;

        @JsonProperty("mode")
        private String mode;

        @JsonProperty("stat")
        private String state;
    }
}
