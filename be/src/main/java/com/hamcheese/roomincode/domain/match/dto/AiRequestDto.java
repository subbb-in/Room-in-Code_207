package com.hamcheese.roomincode.domain.match.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class AiRequestDto {

    @Data
    @Builder
    public static class AiHintRequestDto {

        @JsonProperty("match_id")
        private String matchId;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("problem_id")
        private Long problemId;

        @JsonProperty("answer_code")
        private String sourceCode;

        private String language;

        @JsonProperty("executed_time")
        private Double executedTime;

        @JsonProperty("executed_memory")
        private Integer executedMemory;
    }

    @Getter
    @Builder
    public static class AiRelayRequestDto {
        @JsonProperty("problem_question")
        private String problem;      // 문제 설명

        @JsonProperty("user_code")
        private String code;         // 현재까지 작성된 코드 (User Code)

        private String language;     // 언어 (JAVA 등)
        // 필요한 경우 matchId나 userId를 추가해도 됨
    }

    @Data
    @Builder
    public static class AiFeedbackRequestDto {
        @JsonIgnore
        private String matchId;

        @JsonIgnore
        private String userId;

        @JsonProperty("problem_id")
        private Long problemId;

        @JsonProperty("user_code")
        private String userCode;

        private String language;

        @JsonProperty("time_complexity")
        private Double executedTime;

        @JsonProperty("mem_usage")
        private Integer executedMemory;
    }
}

