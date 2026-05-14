package com.hamcheese.roomincode.domain.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

public class AiResponseDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFeedbackDto {
        private String comment;
        @JsonProperty("refactored_code")
        private String refactoredCode;
        private Map<String, String> recommendation;
    }

    @Getter
    @NoArgsConstructor
    public static class AiRelayResponseDto {
        @JsonProperty("ai_code")
        private String generatedCode; // AI가 이어 쓴 코드
    }

    @Data
    @RequiredArgsConstructor
    public static class AiHintDto{
        @JsonProperty("hint")
        private Map<String, Object> aiHint;   // JSON
    }
}