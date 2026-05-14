package com.hamcheese.roomincode.domain.match.dto;

import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchInitDto {
    private String matchId;
    private MatchLanguage lang;
    private String diff;
    private String algo;
    private int time;
    private String mode;
    private List<String> userIds;
}
