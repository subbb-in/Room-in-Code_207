package com.hamcheese.roomincode.global.security;

import com.hamcheese.roomincode.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            //1. 쿼리 파라미터에서 jwt token 가져와서 검증
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtTokenProvider.validateToken(token)) {
                //2. token에서 userId 추출
                Long userId = jwtTokenProvider.getUserId(token);

                //3. WebSocketSession attribute에 저장
                attributes.put("userId", userId);
                log.info("WebSocket Handshake 성공, userId : {}", userId);
                return true;
            }
        }
        log.warn("WebSocket Handshake 실패: 토큰이 유효하지 않음");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
