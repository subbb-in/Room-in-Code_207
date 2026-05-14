package com.hamcheese.roomincode.domain.match.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiReportEvent {
    private final String matchId;
    private final String payload;
}