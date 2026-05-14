package com.hamcheese.roomincode.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Query String 파싱 헬퍼 메서드
     */
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    /**
     * 클라이언트에게 메시지 전송 헬퍼 메서드
     */
    public static void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(Objects.requireNonNull(json)));
            }
        } catch (IOException e) {
            log.error("WebSocket 메시지 전송 중 오류 발생: {}", e.getMessage());
        }
    }
}