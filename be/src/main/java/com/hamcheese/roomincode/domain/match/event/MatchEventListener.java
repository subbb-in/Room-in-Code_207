package com.hamcheese.roomincode.domain.match.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.handler.MatchWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEventListener {

    private final MatchWebSocketHandler matchWebSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 타이머 틱 이벤트 발생 시 웹소켓으로 브로드캐스팅
     */
    @EventListener
    public void handleMatchTimerTick(MatchTimerTickEvent event) {
        try {
            // 클라이언트에 보낼 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TIMER");
            message.put("matchId", event.getMatchId());
            message.put("remainingTime", event.getRemainingSeconds());
            message.put("currentUserId", event.getCurrentUserId());
            message.put("seq", event.getSequence());

            String payload = objectMapper.writeValueAsString(message);

            // 핸들러를 통해 전송
            matchWebSocketHandler.broadcastToMatch(event.getMatchId(), payload);

        } catch (Exception e) {
            log.error("타이머 이벤트 처리 중 오류 발생: matchId={}", event.getMatchId(), e);
        }
    }

    /**
     * AI 모드 -> AI가 작성한 코드 전송
     * @param event
     */
    @EventListener
    public void handleAiCodeUpdate(AiCodeUpdateEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CODE_UPDATE"); // 프론트와 맞춘 타입
            message.put("matchId", event.getMatchId());
            message.put("sender", "AI_BOT");
            message.put("code", event.getNewCode()); // 변경된 코드

            String payload = objectMapper.writeValueAsString(message);
            matchWebSocketHandler.broadcastToMatch(event.getMatchId(), payload);

            log.info("AI 코드 웹소켓 전송 완료: {}", event.getMatchId());
        } catch (Exception e) {
            log.error("AI 코드 소켓 전송 실패", e);
        }
    }

    /**
     * AI 리포트 -> 웹소켓 전송
     */
    @EventListener
    public void handleAiReport(AiReportEvent event) {
        matchWebSocketHandler.broadcastToMatch(
                event.getMatchId(),
                event.getPayload()
        );
        log.info("AI 리포트 웹소켓 전송 (event)");
    }
}