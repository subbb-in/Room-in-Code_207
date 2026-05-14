package com.hamcheese.roomincode.global.handler;

import com.hamcheese.roomincode.global.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * WebSocket 핸들러의 공통 기능을 제공하는 추상 클래스
 * - 연결 수립/종료 로그 출력
 * - 전송 에러 처리
 * - 쿼리 파라미터 파싱 헬퍼
 */
@Slf4j
public abstract class BaseWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("WebSocket 연결 성공: session={}, uri={}", session.getId(), session.getUri());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket 전송 에러: session={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("WebSocket 연결 종료: session={}, status={}", session.getId(), status);
    }

    /**
     * 세션 URI에서 쿼리 파라미터를 추출하는 헬퍼 메서드
     * 
     * @param session WebSocket 세션
     * @return 쿼리 파라미터 Map (파라미터가 없으면 빈 Map 반환)
     */
    protected Map<String, String> getQueryParams(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return Collections.emptyMap();
        }
        return WebSocketUtil.parseQueryParams(uri.getQuery());
    }
}
