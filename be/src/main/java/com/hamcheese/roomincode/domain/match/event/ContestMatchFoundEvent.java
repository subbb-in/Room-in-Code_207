package com.hamcheese.roomincode.domain.match.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ContestMatchFoundEvent {
    private final List<String> userIds;
    private final String matchId;
}
