package com.hamcheese.roomincode.domain.match.dto;

import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequestDto {
    private String sourceCode;
    private MatchLanguage language;
}
