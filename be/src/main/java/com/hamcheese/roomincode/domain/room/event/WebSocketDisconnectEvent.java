package com.hamcheese.roomincode.domain.room.event;

/**
 * 사용자 퇴장 이벤트
 * RoomWebSocketHandler에서 발행하고 RoomService에서 구독
 * RoomWebSocketHandler와 RoomService 순환참조 막기 위해 이벤트 기반 아키텍처로 리팩토링함
 */
public record WebSocketDisconnectEvent(String roomId, Long userId) {
}
