package com.hamcheese.roomincode.domain.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class Judge0ResultDto {

    @Getter
    @NoArgsConstructor
    public static class Judge0BatchResponse {
        private List<Judge0Response> submissions;
    }

    // Judge0 원본 콜백 응답 dto
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Judge0Response {
        @JsonProperty("stdout")
        private String stdout;
        @JsonProperty("time")
        private String time;
        @JsonProperty("memory")
        private Float memory;
        @JsonProperty("stderr")
        private String stderr;
        @JsonProperty("token")
        private String token;
        @JsonProperty("compile_output")
        private String compileOutput;
        @JsonProperty("message")
        private String message;
        @JsonProperty("status")
        private Status status;

        @Getter
        @NoArgsConstructor
        public static class Status {
            @JsonProperty("id")
            private int id;
            @JsonProperty("description")
            private String description;
        }

        // Base64 디코딩 편의 메서드
        public String getDecodedStdout() {
            return decode(stdout);
        }

        public String getDecodedMessage() {
            return decode(message);
        }

        private String decode(String encoded) {
            if (encoded == null)
                return "";
            try {
                return new String(java.util.Base64.getDecoder().decode(encoded));
            } catch (Exception e) {
                return encoded;
            }
        }
    }

    // 문제 정보
    @Getter
    @AllArgsConstructor
    public static class ProblemList {
        private Float timeLimit;
        private Float memoryLimit;
    }

    // 최종 채점 결과
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MatchResult {
        private boolean success; // 성공 여부
        private String message; // Success / Fail / 진행 중
        private String stdout; // 실행 결과 (내 출력값 결과)

        // 문제 기준
        private String timeLimit;
        private String memoryLimit;

        // 내가 제출한 코드의 실행 결과
        private Double executedTime;
        private Integer executedMemory;
    }

}