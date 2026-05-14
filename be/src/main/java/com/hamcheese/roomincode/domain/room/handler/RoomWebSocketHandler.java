package com.hamcheese.roomincode.domain.room.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.room.dto.RoomMessageDto;
import com.hamcheese.roomincode.domain.room.repository.RoomRedisRepository;
import com.hamcheese.roomincode.domain.room.event.WebSocketDisconnectEvent;
import com.hamcheese.roomincode.global.handler.BaseWebSocketHandler;
import com.hamcheese.roomincode.global.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RoomWebSocketHandler extends BaseWebSocketHandler {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RoomRedisRepository roomRedisRepository;

    // ApplicationEventPublisher를 사용하여 순환 참조 해결
    public RoomWebSocketHandler(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper,
                                RoomRedisRepository roomRedisRepository) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.roomRedisRepository = roomRedisRepository;
    }

    // WebSocket 세션 관리: sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 성공 시 호출
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session); // 공통 로그 출력
        try {
            // 1. Query 파라미터에서 roomId와 userId 추출 (부모 헬퍼 활용)
            Map<String, String> params = getQueryParams(session);
            if (params.isEmpty()) {
                log.warn("WebSocket URI is null or has no params");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            String roomId = params.get("roomId");
            Object attrUserId = session.getAttributes().get("userId");
            String urlUserId = params.get("userId");
            
            // JWT 토큰에서 추출한 userId (인증된 값)
            String userIdStr = attrUserId != null ? String.valueOf(attrUserId) : null;

            // roomId와 userId가 없으면 연결 종료
            if (roomId == null || userIdStr == null) {
                log.warn("Missing roomId or authenticated userId in WebSocket connection");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // JWT userId와 URL userId 불일치 검증 (보안)
            if (urlUserId != null && !urlUserId.equals(userIdStr)) {
                log.error("Security violation: JWT userId({}) != URL userId({})", userIdStr, urlUserId);
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            Long userId = Long.parseLong(userIdStr);

            // 2. 세션 정보 저장
            SessionInfo sessionInfo = new SessionInfo(userId, roomId, session);
            sessionMap.put(session.getId(), sessionInfo);

            log.info("WebSocket connected - SessionId: {}, UserId: {}, RoomId: {}",
                    session.getId(), userId, roomId);

            // 3. 웰컴 메시지 전송 (현재 인원/최대 인원 포함)
            Map<Object, Object> roomInfo = roomRedisRepository.findRoomInfo(roomId);
            int maxCount = 0;
            long currentCount = 0L;

            if (roomInfo != null && !roomInfo.isEmpty()) {
                Object maxObj = roomInfo.get("max");
                if (maxObj != null) {
                    try {
                        maxCount = Integer.parseInt(maxObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                Object cntObj = roomInfo.get("cnt");
                if (cntObj != null) {
                    try {
                        currentCount = Long.parseLong(cntObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                Long participants = roomRedisRepository.getParticipantCount(roomId);
                if (participants != null) {
                    currentCount = participants;
                }
            }

            WebSocketUtil.sendMessage(session, Map.of(
                    "type", "CONNECTED",
                    "message", "Successfully connected to room " + roomId,
                    "data", Map.of(
                            "currentCount", currentCount,
                            "maxCount", maxCount
                    )));

            // 4. 이미 게임이 시작된 방이라면 START 메시지 즉시 전송 (늦게 들어온 유저 처리)
            if (roomInfo != null && !roomInfo.isEmpty()) {
                Object statObj = roomInfo.get("stat");
                if (statObj != null && "PLAYING".equalsIgnoreCase(statObj.toString())) {
                    Map<String, Object> startData = Map.of(
                            "redirectUrl", "/match/" + roomId,
                            "openViduSessionId", roomId
                    );
                    WebSocketUtil.sendMessage(session, Map.of(
                            "type", "START",
                            "roomId", roomId,
                            "senderId", "SYSTEM",
                            "data", startData
                    ));
                    log.info("Late join detected. START sent to UserId: {}, RoomId: {}", userId, roomId);
                }
            }

        } catch (Exception e) {
            log.error("Error establishing WebSocket connection: {}", e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 클라이언트로부터 메시지 수신 시 호출
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received message from session {}: {}", session.getId(), payload);

            // JSON 메시지 파싱
            Map<String, Object> msg = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            String type = (String) msg.get("type");

            SessionInfo sessionInfo = sessionMap.get(session.getId());
            if (sessionInfo == null) {
                log.warn("Session info not found for session: {}", session.getId());
                return;
            }

            // 메시지 타입에 따른 처리
            switch (type) {
                case "PING":
                    // 핑 메시지에 대한 응답
                    WebSocketUtil.sendMessage(session, Map.of("type", "PONG"));
                    break;

                case "SUBSCRIBE":
                    // 구독 요청 처리 (현재는 연결 시 자동 구독)
                    log.info("Subscription confirmed for room: {}", sessionInfo.getRoomId());
                    break;

                case "LEAVE":
                    // 명시적 나가기 요청 처리
                    log.info("User explicitly leaving - UserId: {}, RoomId: {}", 
                            sessionInfo.getUserId(), sessionInfo.getRoomId());
                    
                    // 세션 맵에서 제거
                    sessionMap.remove(session.getId());
                    
                    // 나가기 이벤트 발행
                    eventPublisher.publishEvent(new WebSocketDisconnectEvent(
                            sessionInfo.getRoomId(), sessionInfo.getUserId()));
                    
                    // 세션 종료
                    session.close(CloseStatus.NORMAL);
                    break;

                default:
                    log.warn("Unknown message type: {}", type);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 종료 시 호출 (비명시적 나가기 처리)
     * 네트워크 끊김, 브라우저 종료, 서버 세션 종료 상황에서 호출
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status); // 공통 로그 출력
        try {
            SessionInfo sessionInfo = sessionMap.remove(session.getId());

            if (sessionInfo != null) {
                log.info("WebSocket disconnected - SessionId: {}, UserId: {}, RoomId: {}, Status: {}",
                        session.getId(), sessionInfo.getUserId(), sessionInfo.getRoomId(), status);

                // 비명시적 나가기 처리 - 이벤트 발행으로 위임
                eventPublisher
                        .publishEvent(new WebSocketDisconnectEvent(sessionInfo.getRoomId(), sessionInfo.getUserId()));
            } else {
                log.warn("Session info not found during disconnect: {}", session.getId());
            }

        } catch (Exception e) {
            log.error("Error during WebSocket disconnect: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 전송 Exception 발생 시 호출
     * 프로토콜 위반, 네트워크 패킷 손상 등 기술적 문제 상황에서 호출됨
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        // 공통 에러 처리 (로그 + 세션 종료)는 부모 클래스에서 처리
        super.handleTransportError(session, exception);
    }

    /**
     * 특정 방(roomId)에 접속한 모든 세션에게 메시지 브로드캐스트
     * 
     * @param roomId  대상 방 ID
     * @param message 전송할 메시지 객체 (Map 또는 DTO)
     */
    /**
     * 특정 방(roomId)에 접속한 모든 세션에게 메시지 브로드캐스트
     */
    public void broadcastToRoom(String roomId, RoomMessageDto messageDto) {
        // Map으로 변환
        Map<String, Object> messageMap = objectMapper.convertValue(messageDto, new TypeReference<>() {
        });

        sessionMap.forEach((sessionId, sessionInfo) -> {
            // 해당 방에 속해 있고 세션이 열려있는지 확인
            if (sessionInfo.getRoomId().equals(roomId) && sessionInfo.getSession().isOpen()) {
                WebSocketUtil.sendMessage(sessionInfo.getSession(), messageMap);
            }
        });
    }

    /**
     * 특정 유저의 세션을 찾아 강제로 종료함 (명시적 퇴장 처리용)
     */
    public void closeUserSession(Long userId) {
        sessionMap.forEach((sessionId, sessionInfo) -> {
            if (sessionInfo.getUserId().equals(userId)) {
                try {
                    if (sessionInfo.getSession().isOpen()) {
                        sessionInfo.getSession().close(CloseStatus.NORMAL);
                        log.info("Force closed session for user: {}", userId);
                    }
                } catch (Exception e) {
                    log.error("Error closing session for user {}: {}", userId, e.getMessage());
                }
            }
        });
    }

    /**
     * 현재 roomId에 해당 userId가 실제로 연결되어 있는지 확인
     */
    public boolean isUserConnected(String roomId, Long userId) {
        return sessionMap.values().stream()
                .anyMatch(info -> info.getUserId().equals(userId)
                        && info.getRoomId().equals(roomId)
                        && info.getSession().isOpen());
    }

    /**
     * 세션 정보 내부 클래스
     * roomId별로 유저를 관리하기 위한 클래스
     */
    // private record SessionInfo(Long userId, String roomId) {}
    private static class SessionInfo {
        private final Long userId;
        private final String roomId;
        private final WebSocketSession session;

        public SessionInfo(Long userId, String roomId, WebSocketSession session) {
            this.userId = userId;
            this.roomId = roomId;
            this.session = session;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRoomId() {
            return roomId;
        }

        public WebSocketSession getSession() {
            return session;
        }
    }

}
