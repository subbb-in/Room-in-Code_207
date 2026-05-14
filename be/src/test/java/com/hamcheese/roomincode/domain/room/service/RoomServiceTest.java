package com.hamcheese.roomincode.domain.room.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.service.MatchService;
import com.hamcheese.roomincode.domain.room.dto.RoomMessageDto;
import com.hamcheese.roomincode.domain.room.dto.RoomRequestDto;
import com.hamcheese.roomincode.domain.room.repository.RoomRedisRepository;
import com.hamcheese.roomincode.domain.room.type.RoomEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@SpringBootTest
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRedisRepository roomRedisRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String roomId = "2";
    private final String password = "1111";

    @BeforeEach
    void setUp() {
        // Redis에 테스트용 방 정보 저장
        String keyInfo = "room:" + roomId + ":info";
        Map<String, String> roomInfo = new HashMap<>();
        roomInfo.put("pwd", password);
        roomInfo.put("max", "4");   // 최대 인원
        roomInfo.put("cnt", "0");   // 현재 인원
        roomInfo.put("lang", "Java");
        roomInfo.put("diff", "EASY");
        roomInfo.put("algo", "DFS");
        roomInfo.put("time", "30");
        roomInfo.put("mode", "NORMAL");
        roomInfo.put("stat", "WAITING");

        redisTemplate.opsForHash().putAll(keyInfo, roomInfo);
    }

    @Test
    void testJoinRoom() {
        Long userId = 1001L;

        RoomRequestDto.Join request = new RoomRequestDto.Join();
        request.setPassword(password);

        // 유저 입장
        roomService.joinRoom(roomId, userId, request);

        // 결과 확인
        Map<Object, Object> roomInfo = roomRedisRepository.findRoomInfo(roomId);
        Long currentCount = roomRedisRepository.getParticipantCount(roomId);
        Set<String> participants = roomRedisRepository.getParticipants(roomId);

        System.out.println("방 정보: " + roomInfo);
        System.out.println("현재 인원수: " + currentCount);
        System.out.println("유저가 방에 있는지: " + roomRedisRepository.isUserInRoom(roomId, userId));
        System.out.println("참가자 목록: " + participants);

    }

    @Test
    void waitForFirstUserJoinAndPrintParticipants() throws InterruptedException {
        System.out.println("테스트 준비 완료. 프론트에서 /api/rooms/2/participants 요청을 보내주세요.");

        while (true) {
            Long count = roomRedisRepository.getParticipantCount(roomId);
            if (count != null && count > 0) {
                Set<String> participants = roomRedisRepository.getParticipants(roomId);

                System.out.println("첫 유저 입장 감지!");
                System.out.println("현재 인원: " + count);
                System.out.println("참가자 목록: " + participants);

                break;
            }
            Thread.sleep(1000);
        }
    }

}