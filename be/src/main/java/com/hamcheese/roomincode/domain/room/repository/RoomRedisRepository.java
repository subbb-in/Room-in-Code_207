package com.hamcheese.roomincode.domain.room.repository;

import com.hamcheese.roomincode.domain.room.dto.RoomRequestDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoomRedisRepository {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RoomRedisRepository.class);
    private final StringRedisTemplate redisTemplate;

    private String getKeyInfo(String roomId) {
        return "room:" + roomId + ":info";
    }

    private String getKeyUsers(String roomId) {
        return "room:" + roomId + ":users";
    }

    // --- [조회 관련] ---

    // Room 정보 통쨰로 가져옴
    public Map<Object, Object> findRoomInfo(String roomId) {
        return redisTemplate.opsForHash().entries(getKeyInfo(roomId));
    }

    // 룸 기본 정보에서 특정 필드(time 등) 정보 가져오기
    public String getRoomhMetaValue(String roomId, String field) {
        String key = "room:" + roomId + ":info";
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    // 중복유저 체크 로직
    public boolean isUserInRoom(String roomId, Long userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getKeyUsers(roomId), userId.toString()));
    }

    public Set<String> getParticipants(String roomId) {
        return redisTemplate.opsForSet().members(getKeyUsers(roomId));
    }

    // Room 현재 인원
    public Long getParticipantCount(String roomId) {
        return redisTemplate.opsForSet().size(getKeyUsers(roomId));
    }

    // --- [수정 관련] ---

    /**
     * 유저를 Room(Set)에 추가함
     *
     * @return true if 방에 정상 입장시, false if 입장 못할시
     */
    public boolean addParticipant(String roomId, Long userId) {
        Long result = redisTemplate.opsForSet().add(getKeyUsers(roomId), userId.toString());
        return result != null && result > 0;
    }

    public void removeParticipant(String roomId, Long userId) {
        redisTemplate.opsForSet().remove(getKeyUsers(roomId), userId.toString());
    }

    /**
     * 방 정보(Hash)에서 현재 인원수 증가, users size에서도 갖고올수 있긴함
     * 근데 일단 redis 명세에 room info에 cnt가 있어서 넣음...
     */
    public void incrementUserCount(String roomId) {
        redisTemplate.opsForHash().increment(getKeyInfo(roomId), "cnt", 1);
    }

    // 방 정보에서 현재 인원수 감소
    public void decrementUserCount(String roomId) {
        redisTemplate.opsForHash().increment(getKeyInfo(roomId), "cnt", -1);
    }

    // 방 상태 변경
    public void updateRoomStatus(String roomId, String status) {
        redisTemplate.opsForHash().put(getKeyInfo(roomId), "stat", status);
    }

    /**
     * 방 저장 로직 (MODE 별 통합)
     */
    public void saveRoom(String roomId, Map<String, String> roomInfo) {
        String key = getKeyInfo(roomId);
        String usersKey = getKeyUsers(roomId);

        // 1. Hash 데이터 저장
        redisTemplate.opsForHash().putAll(key, roomInfo);

        // 2. FRIENDS 모드일 경우에만 공개 목록에 추가
        String mode = roomInfo.get("mode");
        if ("FRIENDS".equalsIgnoreCase(mode)) {
            addToRoomList(roomId, mode);
        }

        // 3. TTL 15분 설정 (info + users)
        redisTemplate.expire(key, Duration.ofMinutes(15));
        redisTemplate.expire(usersKey, Duration.ofMinutes(15));
    }

    // 방 공개목록에 추가
    public void addToRoomList(String roomId, String mode) {

        String listKey = "rooms:" + mode;
        redisTemplate.opsForSet().add(listKey, roomId);
    }

    // 방 나가기 (현재 인원 수 1 감소시키고, 감소된 인원수 반환)
    public Long decrementParticipantCount(String roomId) {
        String key = "room:" + roomId + ":info";

        return redisTemplate.opsForHash().increment(key, "cnt", -1);
    }

    // 방 삭제 (정보 삭제 + 목록 제거)
    public void deleteRoom(String roomId) {
        String infoKey = getKeyInfo(roomId);
        String usersKey = getKeyUsers(roomId);

        // 방 정보/유저 목록 삭제
        redisTemplate.delete(infoKey);
        redisTemplate.delete(usersKey);

        // 방 공개목록에서 방ID 제거 (FRIENDS/CONTEST 등 전체 목록에서 제거)
        redisTemplate.opsForSet().remove("rooms:FRIENDS", roomId);
        redisTemplate.opsForSet().remove("rooms:CONTEST", roomId);
        redisTemplate.opsForSet().remove("rooms:public", roomId);
    }

    // 방 목록 조회
    public List<Map<Object, Object>> findAllRoomDetails(String mode) {
        try {
            // mode가 null이면 FRIENDS로 기본값 설정
            String listKey = "rooms:" + (mode != null ? mode : "FRIENDS");
            log.info("방 목록 조회 시작 - listKey: {}", listKey);
            
            // 활성방 목록 조회
            Set<String> waitingRoomIds = redisTemplate.opsForSet().members(listKey);

            if (waitingRoomIds == null || waitingRoomIds.isEmpty()) {
                log.info("조회된 방이 없음 - listKey: {}", listKey);
                return Collections.emptyList();
            }

            log.info("조회된 방 개수: {} - listKey: {}", waitingRoomIds.size(), listKey);

            // 각 ID를 순회하며 "room:{id}:info" 키의 hash 데이터를 가져옴
            return waitingRoomIds.stream()
                    .map(roomId -> {
                        String key = "room:" + roomId + ":info";
                        return redisTemplate.opsForHash().entries(key);
                    })
                    .filter(map -> !map.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("방 목록 조회 중 오류 발생 - mode: {}", mode, e);
            return Collections.emptyList();
        }
    }
}