package com.hamcheese.roomincode.domain.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Judge0CallbackDto {
    @JsonProperty("token")
    private String token;

    @JsonProperty("stdout")
    private String stdout;

    @JsonProperty("stderr")
    private String stderr;

    @JsonProperty("compile_output")
    private String compileOutput;

    @JsonProperty("message")
    private String message;

    @JsonProperty("time")
    private String time;

    @JsonProperty("memory")
    private Float memory;

    @JsonProperty("status")
    private Status status;

    @Data
    @NoArgsConstructor
    public static class Status {
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("description")
        private String description;
    }
}
