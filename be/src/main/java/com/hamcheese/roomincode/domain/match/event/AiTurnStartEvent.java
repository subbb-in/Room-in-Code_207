package com.hamcheese.roomincode.domain.match.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiTurnStartEvent {
    private String matchId;
    private int sequence;
}