package com.hamcheese.roomincode.domain.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    private Long problemId;
    private String question;
    private String solutions;
    private String difficulty; // Enum managed as String in DB
    private String source;
    private String url;
    private String timeLimit;
    private String memoryLimit;
    private String inputOutput; // JSON string
    private String expectedAuxSpace;
    private String expectedTimeComplexity;
}
