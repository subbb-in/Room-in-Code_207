package com.hamcheese.roomincode.domain.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemHint {
    private Long hintId;
    private Integer sequence;
    private String content;
    private Long problemId;
}
