package com.hamcheese.roomincode.domain.match.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 매치 타이머 틱 이벤트
 * MatchTimerService가 발행하고 MatchEventListener(혹은 Handler)가 구독
 */
@Getter
@Builder
@AllArgsConstructor
public class MatchTimerTickEvent {
    private final String matchId;
    private final long remainingSeconds;
    private final String currentUserId;
    private final int sequence;
}