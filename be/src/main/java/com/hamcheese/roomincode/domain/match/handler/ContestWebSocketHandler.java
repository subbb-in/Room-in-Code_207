package com.hamcheese.roomincode.domain.match.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.event.ContestMatchFoundEvent;
import com.hamcheese.roomincode.domain.match.service.ContestService;
import com.hamcheese.roomincode.global.handler.BaseWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContestWebSocketHandler extends BaseWebSocketHandler {

    private final ContestService contestService;
    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("Contest WS Connected: User {}", userId);

            // 늦게 연결된 유저에게 매치 시작 메시지 전송
            String pendingMatchId = contestService.consumePendingMatch(userId);
            if (pendingMatchId != null && session.isOpen()) {
                Map<String, Object> startData = Map.of(
                        "redirectUrl", "/friends/" + pendingMatchId,
                        "openViduSessionId", pendingMatchId
                );
                Map<String, Object> payload = Map.of(
                        "type", "START",
                        "roomId", pendingMatchId,
                        "senderId", "SYSTEM",
                        "data", startData
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                log.info("Late join detected. START sent to UserId: {}, MatchId: {}", userId, pendingMatchId);
            }
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();

        // 1. 우선 JsonNode로 읽어서 type 확인
        JsonNode jsonNode = objectMapper.readTree(payload);
        JsonNode typeNode = jsonNode.get("type");

        if (typeNode == null) {
            log.warn("Invalid payload: missing type");
            return;
        }

        String type = typeNode.asText();

        switch (type.toUpperCase()) {
            case "PING":
                session.sendMessage(new TextMessage("PONG"));
                log.debug("Received PING from user {}, responded with PONG", session.getAttributes().get("userId"));
                break;

            default:
                log.debug("Unhandled message from user {}: {}", session.getAttributes().get("userId"), payload);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        Long userId = (Long) session.getAttributes().get("userId");

        if (userId != null) {
            userSessions.remove(userId);
            log.info("User {} disconnected from Contest WS. Cancelling all queues.", userId);
            contestService.cancelAllQueues(userId);
        }
    }

    @EventListener
    public void handleContestMatchFound(ContestMatchFoundEvent event) {
        String matchId = event.getMatchId();

        Map<String, Object> startData = Map.of(
            "redirectUrl", "/friends/" + matchId,
            "openViduSessionId", matchId
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "START");
        payload.put("roomId", matchId);
        payload.put("senderId", "SYSTEM");
        payload.put("data", startData);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(jsonPayload);

            for (String userIdStr : event.getUserIds()) {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    WebSocketSession session = userSessions.get(userId);

                    if (session != null && session.isOpen()) {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid userId format in event: {}", userIdStr);
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast MATCH_FOUND", e);
        }
    }
}
