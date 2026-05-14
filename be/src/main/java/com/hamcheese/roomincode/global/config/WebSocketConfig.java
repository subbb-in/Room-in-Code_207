package com.hamcheese.roomincode.global.config;

import com.hamcheese.roomincode.domain.match.handler.ContestWebSocketHandler;
import com.hamcheese.roomincode.domain.match.handler.MatchWebSocketHandler;
import com.hamcheese.roomincode.domain.room.handler.RoomWebSocketHandler;
import com.hamcheese.roomincode.global.security.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@RequiredArgsConstructor
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;
    private final MatchWebSocketHandler matchWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final ContestWebSocketHandler contestWebSocketHandler;

    /**
     * Raw WebSocket 핸들러 등록 (커스텀 핸들러 사용)
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 방 입장/나가기 및 상태 관리
        registry.addHandler(roomWebSocketHandler, "/ws/rooms")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 매치 전용 핸들러 등록
        registry.addHandler(matchWebSocketHandler, "/ws/match")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");

        // Contest 전용 핸들러 등록
        registry.addHandler(contestWebSocketHandler, "/ws/contest")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");

    }

    /**
     * STOMP over WebSocket 엔드포인트 등록 (SimpMessagingTemplate 지원)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp")
                .setAllowedOriginPatterns("http://localhost:5173")
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정
     * 클라이언트가 구독할 수 있는 prefix 접두사 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic : 1:N 통신용
        // /queue : 1:1 통신용
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트에서 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}
