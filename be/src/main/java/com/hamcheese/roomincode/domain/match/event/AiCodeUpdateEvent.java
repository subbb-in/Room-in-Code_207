package com.hamcheese.roomincode.domain.match.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiCodeUpdateEvent {
    private String matchId;
    private String newCode;
}
