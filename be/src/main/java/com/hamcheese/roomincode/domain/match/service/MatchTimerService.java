package com.hamcheese.roomincode.domain.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.event.AiTurnStartEvent;
import com.hamcheese.roomincode.domain.match.event.MatchTimerTickEvent;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.problem.dto.Problem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchTimerService {

    private final MatchRedisRepository matchRedisRepository;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;

//    @Lazy
//    private final MatchWebSocketHandler matchWebSocketHandler;
    //@Lazy 말고 이벤트 기반 아키텍처로 리팩토링함
    private final ApplicationEventPublisher eventPublisher;

    // 매치(matchId)별 스케줄러 관리
    private final Map<String, ScheduledFuture<?>> matchSchedulers = new ConcurrentHashMap<>();

    private static final String AI_BOT = "AI_BOT";

    /**
     * 턴 시작 (최초 시작 or 다음 턴)
     */
    public void startTurn(String matchId, String userId, int sequence) {

        // 기존 타이머가 있다면 취소 (이전 턴 타이머는 항상 정리함)
        cancelTimer(matchId);

        // 1. 턴 시간 조회
        String timeStr = matchRedisRepository.getMatchMetaValue(matchId, "time");

        int turnDuration = (timeStr != null) ? Integer.parseInt(timeStr) : 60; // 기본값 60초
        // 종료 시각 계산 = 현재 시각 + 턴 시간(Millis)
        long endAt = System.currentTimeMillis() + (turnDuration * 1000L);

        // 2. Redis에 턴 정보 업데이트 (게임 id, 사용자 id, 순서)
        matchRedisRepository.updateTurnInfo(matchId, userId, endAt, sequence);

        log.info("턴 시작: matchId={}, user={}, seq={}, duration={}s", matchId, userId, sequence, turnDuration);

        // 3. 1초마다 실행되는 스케줄러 등록 (Virtual Thread 사용)
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> broadcastTimerLoop(matchId, userId, sequence, endAt),
                Instant.now(),
                Duration.ofSeconds(1)
        );
        matchSchedulers.put(matchId, future);

        // 4. AI라면 타이머 세팅 후 실행
        if (AI_BOT.equals(userId)) {
            log.info("AI 턴 감지: matchId={}", matchId);
            // MatchService가 이 이벤트를 듣고 AI 로직을 실행함
            eventPublisher.publishEvent(new AiTurnStartEvent(matchId, sequence));
        }
    }

    /**
     * 1초마다 실행되는 루프
     */
    private void broadcastTimerLoop(String matchId, String currentUserId, int sequence, long endAt) {
        try {
            long remainingMillis = endAt - System.currentTimeMillis();
            long remainingSeconds = remainingMillis / 1000;
            // 직접 핸들러를 호출하지 않고 이벤트 발행
            if (remainingSeconds >= 0) {
                eventPublisher.publishEvent(MatchTimerTickEvent.builder()
                        .matchId(matchId)
                        .remainingSeconds(remainingSeconds)
                        .currentUserId(currentUserId)
                        .sequence(sequence)
                        .build());

            } else {
                // 시간 초과 -> 다음 턴으로 전환
                log.info("턴 시간 종료: matchId={}", matchId);
                rotateTurn(matchId, currentUserId, sequence);
            }
        } catch (Exception e) {
            log.error("타이머 에러 matchId={}", matchId, e);
            cancelTimer(matchId); // 에러 발생 시 안전하게 타이머 중지
        }
    }

    /**
     * 다음 턴 유저 계산 및 전환
     */
    // private -> public 변경
    // MatchService에서 AI 턴 끝나면 이 메서드 호출해야 함
    public void rotateTurn(String matchId, String currentUserId, int currentSeq) {
        // 1. matchId 기반 참여자 목록 조회
        Set<String> usersSet = matchRedisRepository.getMatchUsers(matchId);
        // 사용자 없을 경우 스케줄러 정리
        if (usersSet == null || usersSet.isEmpty()) {
            cancelTimer(matchId);
            return;
        }

        // UUID 기준 사전 순 정렬
        List<String> sortedUsers = new ArrayList<>(usersSet);
        Collections.sort(sortedUsers);

        // 2. 다음 유저 찾기
//        int nextSeq = currentSeq + 1;
//        int nextIndex = nextSeq % sortedUsers.size(); // 0 -> 1 -> 2 -> 3 -> 0 ...
//        String nextUserId = sortedUsers.get(nextIndex);

        // (사용자 ID에 따라 턴이 한 쪽만 돌아갈 수 있음)
        // 현재 유저의 인덱스 찾아서 다음 인덱스 계산하는 방식으로 수정
        int currentIndex = sortedUsers.indexOf(currentUserId);
        if (currentIndex == -1) {
            log.error("현재 유저({})가 참여자 목록에 없습니다.", currentUserId);
            return; // 혹은 0번 인덱스로 강제 지정
        }

        int nextIndex = (currentIndex + 1) % sortedUsers.size(); // 현재 위치 기반 다음 순서
        String nextUserId = sortedUsers.get(nextIndex);

        int nextSeq = currentSeq + 1; // 시퀀스는 계속 증가

        // 3. 다음 턴 시작
        startTurn(matchId, nextUserId, nextSeq);
    }

    /**
     * 타이머 중지 (게임 종료 시 호출)
     */
    public void cancelTimer(String matchId) {
        ScheduledFuture<?> future = matchSchedulers.remove(matchId);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

}