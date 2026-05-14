package com.hamcheese.roomincode.global.infra.openvidu;

import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenViduService {

    private final OpenVidu openVidu;

    // 세션 생성
    public String createSession() throws OpenViduJavaClientException, OpenViduHttpException {
        SessionProperties properties = new SessionProperties.Builder().build();
        Session session = openVidu.createSession(properties);
        return session.getSessionId();
    }

    // matchId를 세션 ID로 사용
    public String createSessionWithId(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        // 이미 존재하는 세션인지 확인
        openVidu.fetch();
        Session existingSession = openVidu.getActiveSession(sessionId);
        if (existingSession != null) {
            return existingSession.getSessionId();
        }
        
        SessionProperties properties = new SessionProperties.Builder()
                .customSessionId(sessionId)
                .build();
        Session session = openVidu.createSession(properties);
        return session.getSessionId();
    }

    // 토큰 발행
    public String generateToken(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openVidu.getActiveSession(sessionId); // 매번 새로 가져옴
        if (session == null) {
            throw new RuntimeException("세션을 찾을 수 없습니다.");
        }
        ConnectionProperties properties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .build();
        return session.createConnection(properties).getToken();
    }

    // 세션 삭제
    public void closeSession(String sessionId) {
        try {
            openVidu.fetch();
            Session session = openVidu.getActiveSession(sessionId);
            if (session != null) {
                session.close(); // 해당 세션의 모든 연결 끊기 및 세션 삭제
            } else {
                // 세션이 이미 닫혔거나 존재하지 않음
                // 정상 상황일 수 있으므로 로그만 남김
            }
        } catch (Exception e) {
            // 세션 종료 중 예외 발생해도 무시 (이미 종료되었거나 네트워크 오류)
            // 로그만 남기고 계속 진행
        }
    }
}
