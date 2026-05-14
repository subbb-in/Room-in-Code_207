package com.hamcheese.roomincode.domain.room.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.dto.MatchInitDto;
import com.hamcheese.roomincode.domain.match.service.MatchService;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.room.dto.RoomMessageDto;
import com.hamcheese.roomincode.domain.room.dto.RoomRequestDto;
import com.hamcheese.roomincode.domain.room.dto.RoomResponseDto;
import com.hamcheese.roomincode.domain.room.handler.RoomWebSocketHandler;
import com.hamcheese.roomincode.domain.room.repository.RoomRedisRepository;
import com.hamcheese.roomincode.domain.room.type.RoomEventType;
import com.hamcheese.roomincode.domain.room.event.WebSocketDisconnectEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRedisRepository roomRedisRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;
    private final MatchService matchService;
    private final RoomWebSocketHandler roomWebSocketHandler;

    /**
     * 방 입장 처리 (REST)
     */
    public void joinRoom(String roomId, Long userId, RoomRequestDto.Join request) {
        log.info("=== joinRoom 시작 - RoomId: {}, UserId: {} ===", roomId, userId);
        
        // 1. 방 정보 조회하고 존재여부 확인
        Map<Object, Object> roomInfo = roomRedisRepository.findRoomInfo(roomId);
        if (roomInfo == null || roomInfo.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않거나 만료된 방입니다");
        }
        
        log.info("방 정보 확인 완료 - RoomId: {}, 현재 인원: {}/{}", 
                 roomId, roomInfo.get("cnt"), roomInfo.get("max"));

        // 2. 비밀번호 검증
        String roomPwd = (String) roomInfo.get("pwd");
        if (roomPwd != null && !roomPwd.isEmpty() && !roomPwd.equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // 3. 중복 입장 관리 (Idempotent 처리)
        boolean alreadyInRedis = roomRedisRepository.isUserInRoom(roomId, userId);
        log.info("Redis 멤버십 체크 - UserId: {}, RoomId: {}, alreadyInRedis: {}", userId, roomId, alreadyInRedis);
        
        if (alreadyInRedis) {
            boolean connected = roomWebSocketHandler.isUserConnected(roomId, userId);
            log.info("WebSocket 연결 체크 - UserId: {}, RoomId: {}, connected: {}", userId, roomId, connected);
            
            if (connected) {
                // 이미 정상적으로 입장한 상태 - Idempotent하게 성공 응답 반환
                log.info("이미 입장 완료된 유저. Idempotent 처리 - UserId: {}, RoomId: {}", userId, roomId);
                Long currentCount = roomRedisRepository.getParticipantCount(roomId);
                int maxUser = Integer.parseInt(roomInfo.get("max").toString());
                
                Map<String, Object> joinData = new HashMap<>();
                joinData.put("currentCount", currentCount);
                joinData.put("maxCount", maxUser);
                
                // JOIN 메시지는 브로드캐스트하지 않음 (이미 입장했으므로)
                return; // 정상 종료
            } else {
                // Redis에는 있지만 WebSocket은 끊긴 상태 - Stale 데이터 정리
                log.info("Stale membership detected. Cleaning up user: {}, Room: {}", userId, roomId);
                roomRedisRepository.removeParticipant(roomId, userId);

                Object cntObj = roomInfo.get("cnt");
                if (cntObj != null) {
                    try {
                        long cnt = Long.parseLong(cntObj.toString());
                        if (cnt > 0) {
                            roomRedisRepository.decrementUserCount(roomId);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 4. 정원 초과 확인 및 유저 추가
        int maxUser = Integer.parseInt(roomInfo.get("max").toString());
        boolean isNewUser = roomRedisRepository.addParticipant(roomId, userId);
        Long currentCount = roomRedisRepository.getParticipantCount(roomId);

        log.info("유저 추가 결과 - maxUser: {}, isNewUser: {}, currentCount: {}", maxUser, isNewUser, currentCount);

        if (currentCount > maxUser) {
            // 정원 초과시 롤백
            roomRedisRepository.removeParticipant(roomId, userId);
            throw new IllegalStateException("방 정원 초과입니다");
        }

        if (isNewUser) {
            roomRedisRepository.incrementUserCount(roomId);
            currentCount = roomRedisRepository.getParticipantCount(roomId);
            log.info("{}방에 {} 번 유저 접속", roomId, userId);
        }

        // 5.
        // RoomEventType JOIN을 웹소켓으로 전송해야
        Map<String, Object> joinData = new HashMap<>();
        joinData.put("currentCount", currentCount);
        joinData.put("maxCount", maxUser);

        broadcastMessage(roomId, RoomMessageDto.builder()
                .type(RoomEventType.JOIN)
                .roomId(roomId)
                .senderId(userId.toString())
                .data(joinData)
                .build());

        // 6. 게임 시작 조건 충족하면 시작
        if (currentCount == maxUser) {
            startMatch(roomId);
        }
    }

    /**
     * 게임 시작 처리
     * 여기서부터 Room의 역할이 끝나고 Match로 넘어감, roomId는 그대로 이름만 바꿔서 이어감
     */
    private void startMatch(String roomId) {

        // 1. 방 설정 정보 가져오기 (Redis)
        Map<Object, Object> roomInfo = roomRedisRepository.findRoomInfo(roomId);

        if (roomInfo == null || roomInfo.isEmpty()) {
            throw new IllegalStateException("방 정보를 찾을 수 없습니다");
        }

        // 2. 이제부터 matchId 차례
        String matchId = roomId;

        // 3. Room 정보 -> MatchInitDto 변환
        java.util.Set<String> participants = roomRedisRepository.getParticipants(roomId);
        List<String> userIds = new java.util.ArrayList<>(participants);

        // + AI 모드라면 AI 봇을 상대방으로 강제로 추가
        // Roomservice에서는 실제 유저만 넘기고 MatchService 내부에서 모드 확인 후 AI_BOT 추가로 리팩토링
//        if("AI".equalsIgnoreCase((String) roomInfo.get("mode"))) {
//            userIds.add("AI_BOT");
//        }

        MatchInitDto matchInitDto = MatchInitDto.builder()
                .matchId(matchId)
                .lang(MatchLanguage.fromString((String) roomInfo.get("lang")))
                .diff((String) roomInfo.get("diff"))
                .algo((String) roomInfo.get("algo"))
                .time(Integer.parseInt((String) roomInfo.get("time")))
                .mode((String) roomInfo.get("mode"))
                .userIds(userIds)
                .build();

        log.info("Room {} 정원 충족. Transitioning to Match {}.", roomId, matchId);

        // 5. 상태 변경
        roomRedisRepository.updateRoomStatus(roomId, "PLAYING");

        // 6. 게임 서비스에 매치 초기화 위임
        matchService.initializeMatch(matchInitDto);

        // 7. WebSocket 통신을 통해 클라이언트에 리다이렉트 지시 + OpenVidu 세션 정보 전달
        // 클라이언트는 이 메시지 받으면 '/match/{matchId}' 화면으로 이동함
        Map<String, Object> startData = new HashMap<>();
        startData.put("redirectUrl", "/match/" + matchId);
        startData.put("openViduSessionId", matchId); // matchId를 세션 ID로 사용

        broadcastMessage(roomId, RoomMessageDto.builder()
                .type(RoomEventType.START)
                .roomId(roomId) // 클라이언트는 아직 roomId를 구독 중이므로 roomId로 보냄
                .senderId("SYSTEM")
                .data(startData)
                .build());

    }

    // 브로드캐스트 메세지
    private void broadcastMessage(String roomId, RoomMessageDto message) {
        // simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        roomWebSocketHandler.broadcastToRoom(roomId, message);
    }

    // 방 목록 조회
    public List<RoomResponseDto.Info> getRoomList(RoomRequestDto.List dto) {
        // 레디스에서 활성화된 모든 방의 상세 정보(Hash 데이터)를 가져옴
        List<Map<Object, Object>> rawRoomMaps = roomRedisRepository.findAllRoomDetails(dto.getMode());

        // 파라미터로 넘어온 mode를 기준으로 필터링
        return rawRoomMaps.stream()
                // map -> DTO 변환
                .map(map -> objectMapper.convertValue(map, RoomResponseDto.Info.class))

                // 대기중(WAITING)인 방만 노출
                .filter(room -> "WAITING".equalsIgnoreCase(room.getState()))

                // 파라미터로 받은 mode와 일치하는지 확인
                .filter(room -> dto.getMode() == null ||
                        room.getMode().equalsIgnoreCase(dto.getMode()))

                // 나머지 동적 필터들 (언어, 난이도 등)
                .filter(room -> dto.getLanguage() == null || room.getLanguage().equals(dto.getLanguage()))
                .filter(room -> dto.getDifficulty() == null
                        || room.getDifficulty().equalsIgnoreCase(dto.getDifficulty()))
                .filter(room -> dto.getAlgorithm() == null || room.getAlgorithm().equalsIgnoreCase(dto.getAlgorithm()))

                .collect(Collectors.toList());
    }

    // 대기방 나가기
    public RoomResponseDto.RoomLeaveResponse leaveRoom(String roomId, Long userId) {
        // // 1. 인원 수 감소
        // Long remainingCount = roomRedisRepository.decrementParticipantCount(roomId);
        Long remainingCount = handleUserLeave(roomId, userId);

        roomWebSocketHandler.closeUserSession(userId);

        // // 2. 남은 인원이 0명이면 방 폭파 -> 삭제되었다는 응답 리턴
        // if (remainingCount <= 0) {
        // //roomRedisRepository.deleteRoom(roomId);
        //
        // return RoomResponseDto.RoomLeaveResponse.builder()
        // .message("마지막 인원이 퇴장하여 방이 삭제되었습니다.")
        // .isDeleted(true)
        // .remainingCount(0L)
        // .build();
        // }

        // 3. 인원이 남아있으면 -> 그냥 나갔다는 응답 리턴
        return RoomResponseDto.RoomLeaveResponse.builder()
                .message("성공적으로 방을 나갔습니다.")
                .isDeleted(false)
                .remainingCount(remainingCount)
                .build();

    }

    /**
     * 유저 나가기 공통 로직 (명시적/비명시적 모두 처리)
     *
     * @param roomId 방 ID
     * @param userId 유저 ID
     * @return 남은 인원 수 (방이 삭제된 경우 0)
     */
    @EventListener
    public void handleUserLeaveEvent(WebSocketDisconnectEvent event) {
        handleUserLeave(event.roomId(), event.userId());
    }

    public Long handleUserLeave(String roomId, Long userId) {
        try {
            // 1. 방 정보 확인
            Map<Object, Object> roomInfo = roomRedisRepository.findRoomInfo(roomId);
            if (roomInfo == null || roomInfo.isEmpty()) {
                log.warn("방을 찾을 수 없습니다. RoomId: {}", roomId);
                return 0L;
            }

            // 2. 유저가 실제로 방에 있는지 확인
            if (!roomRedisRepository.isUserInRoom(roomId, userId)) {
                log.warn("유저가 방에 없습니다. UserId: {}, RoomId: {}", userId, roomId);
                return roomRedisRepository.getParticipantCount(roomId);
            }

            // 3. 유저 제거 및 인원 수 감소
            roomRedisRepository.removeParticipant(roomId, userId);
            roomRedisRepository.decrementUserCount(roomId);

            // 4. 남은 인원 수 확인
            Long remainingCount = roomRedisRepository.getParticipantCount(roomId);

            log.info("유저 퇴장 처리 완료 - UserId: {}, RoomId: {}, 남은 인원: {}",
                    userId, roomId, remainingCount);

            // 5. LEAVE 이벤트 브로드캐스트
            int maxCount = Integer.parseInt(roomInfo.get("max").toString());

            Map<String, Object> leaveData = new HashMap<>();
            leaveData.put("currentCount", remainingCount);
            leaveData.put("maxCount", maxCount);

            broadcastMessage(roomId, RoomMessageDto.builder()
                    .type(RoomEventType.LEAVE)
                    .roomId(roomId)
                    .senderId(userId.toString())
                    .data(leaveData)
                    .build());

            // 6. 남은 인원이 0명이면 방 삭제
            if (remainingCount <= 0) {
                roomRedisRepository.deleteRoom(roomId);
                log.info("마지막 인원 퇴장으로 방 삭제 - RoomId: {}", roomId);
            }
            // return값은 추후 log 등을 위해 남겨두는 것으로 함
            return remainingCount;

        } catch (Exception e) {
            log.error("유저 퇴장 처리 중 오류 발생 - UserId: {}, RoomId: {}, Error: {}",
                    userId, roomId, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * FREINDS 모드 방 생성
     */
    public RoomResponseDto.RoomCreateResponse createRoom(RoomRequestDto.RoomCreateRequest requestDto, Long userId) {

        // 1. UUID로 방 생성 (임시방 만들때는 Increment 보다 UUID가 더 나음)
        String roomId = UUID.randomUUID().toString();

        // 2. 데이터 삽입
        Map<String, String> roomInfo = new HashMap<>();
        roomInfo.put("id", roomId);
        roomInfo.put("title", requestDto.getTitle());
        roomInfo.put("pwd", requestDto.getPassword());
        roomInfo.put("max", String.valueOf(requestDto.getCapacity()));
        roomInfo.put("cnt", "0");
        roomInfo.put("lang", requestDto.getLanguage().name());
        roomInfo.put("diff", requestDto.getDifficulty().name());
        roomInfo.put("algo", requestDto.getAlgorithm());
        roomInfo.put("time", String.valueOf(requestDto.getTurnTime()));
        roomInfo.put("mode", "FRIENDS");
        roomInfo.put("stat", "WAITING");

        // 3. 레디스에 저장
        roomRedisRepository.saveRoom(roomId, roomInfo);

        // 4. 입장 처리
        roomRedisRepository.addParticipant(roomId, userId);
        roomRedisRepository.incrementUserCount(roomId);

        return RoomResponseDto.RoomCreateResponse.builder()
                .roomId(roomId)
                .title(requestDto.getTitle())
                .language(requestDto.getLanguage())
                .algorithm(requestDto.getAlgorithm())
                .difficulty(requestDto.getDifficulty().name())
                .capacity(requestDto.getCapacity())
                .count(1)
                .state("WAITING")
                .mode("FRIENDS")
                .build();
    }

    /**
     * AI 연습모드 방 생성 및 즉시 입장 (생성 즉시 게임 시작 상태로 전환)
     */
    public RoomResponseDto.RoomCreateResponse createAiRoom(RoomRequestDto.AiRoomCreateRequest aiRequestDto, Long userId) {

        // 1. UUID로 방 생성
        String roomId = UUID.randomUUID().toString();

        // 2. 레디스 저장용 데이터 생성
        Map<String, String> roomInfo = new HashMap<>();
        roomInfo.put("id", roomId);
        roomInfo.put("title", "AI 연습모드 - " + userId);
        roomInfo.put("pwd", "");
        roomInfo.put("max", "2");
        roomInfo.put("cnt", "0");
        roomInfo.put("lang", aiRequestDto.getLanguage().name());
        roomInfo.put("diff", aiRequestDto.getDifficulty().name());
        roomInfo.put("algo", aiRequestDto.getAlgorithm());
        roomInfo.put("time", String.valueOf(aiRequestDto.getTurnTime()));
        roomInfo.put("mode", "AI");
        roomInfo.put("stat", "WAITING");

        // 3. 레디스에 저장
        roomRedisRepository.saveRoom(roomId, roomInfo);

        // 4. 입장 처리
        roomRedisRepository.addParticipant(roomId, userId);
        roomRedisRepository.incrementUserCount(roomId);

        // 4. AI 모드는 대기 없이 바로 게임 시작 로직(startMatch) 호출
        // AI 모드이므로 인원체크 안하고 강제 실행
        startMatch(roomId);

        return RoomResponseDto.RoomCreateResponse.builder()
                .roomId(roomId)
                .title("AI 연습모드 - " + userId)
                .language(aiRequestDto.getLanguage())
                .algorithm(aiRequestDto.getAlgorithm())
                .difficulty(aiRequestDto.getDifficulty().name())
                .capacity(2)
                .count(1)
                .state("PLAYING")       // 즉시 게임 시작 상태
                .mode("AI")
                .build();
    }
}
