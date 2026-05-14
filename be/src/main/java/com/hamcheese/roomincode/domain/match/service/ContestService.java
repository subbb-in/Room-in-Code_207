package com.hamcheese.roomincode.domain.match.service;

import com.hamcheese.roomincode.domain.match.dto.MatchInitDto;
import com.hamcheese.roomincode.domain.match.repository.ContestQueueRepository;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.room.dto.RoomMessageDto;
import com.hamcheese.roomincode.domain.room.type.RoomEventType;
import com.hamcheese.roomincode.domain.match.event.ContestMatchFoundEvent;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestQueueRepository contestQueueRepository;
    private final MatchService matchService;
    private final UserDao userDao;
    private final ApplicationEventPublisher eventPublisher;

    // Tier Logic
    private String calculateTier(int xp) {
        if (xp >= 2500)
            return "MASTER";
        if (xp >= 1500)
            return "DIAMOND";
        if (xp >= 1000)
            return "PLATINUM";
        if (xp >= 600)
            return "GOLD";
        if (xp >= 300)
            return "SILVER";
        return "BRONZE";
    }

    // 티어에 난이도 대응
    private String mapTierToDifficulty(String tier) {
        switch (tier) {
            case "MASTER":
            case "DIAMOND":
                return "VERY_HARD";
            case "PLATINUM":
                return "HARD";
            case "GOLD":
                return "MEDIUM_HARD";
            case "SILVER":
                return "MEDIUM";
            default:
                return "EASY"; // BRONZE
        }
    }

    @Transactional
    public void joinQueue(Long userId, MatchLanguage lang) {
        // 1. Get User XP & Tier
        Integer xp = userDao.findXpByUserId(userId);
        if (xp == null) {
            throw new IllegalArgumentException("사용자 XP 정보를 찾을 수 없습니다.");
        }

        String tier = calculateTier(xp);

        log.info("유저 {} (XP: {}, Tier: {}) {} 큐 진입", userId, xp, tier, lang.getValue());

        // 2. Add to Redis Queue
        contestQueueRepository.addToQueue(lang.getValue(), tier, userId);

        // 3. Check if matching is possible
        checkAndMakeMatch(lang, tier);
    }

    @Transactional
    public void cancelQueue(Long userId, MatchLanguage lang) {
        Integer xp = userDao.findXpByUserId(userId);
        if (xp == null) {
            log.warn("Cancel Queue: User XP not found for {}", userId);
            return;
        }
        String tier = calculateTier(xp);

        contestQueueRepository.removeFromQueue(lang.getValue(), tier, userId);
        log.info("유저 {} 큐 취소 (Lang: {})", userId, lang.getValue());
    }

    @Transactional
    public void cancelAllQueues(Long userId) {
        // WebSocket 끊김 시 어떤 언어로 대기 중인지 모르므로 가능한 모든 언어 큐에서 제거 시도
        // 혹은 Redis에 별도로 저장해두는 방법도 있지만 여기선 반복 시도

        Integer xp = userDao.findXpByUserId(userId);
        if (xp == null)
            return;
        String tier = calculateTier(xp);

        for (MatchLanguage lang : MatchLanguage.values()) {
            contestQueueRepository.removeFromQueue(lang.getValue(), tier, userId);
        }
        log.info("유저 {} 모든 대기열 취소 시도 완료", userId);
    }

    private void checkAndMakeMatch(MatchLanguage lang, String tier) {
        // 1. 큐 사이즈 확인
        Long size = contestQueueRepository.getQueueSize(lang.getValue(), tier);

        if (size >= 4) {
            // 2. 4명모이면 pop
            Set<String> userIds = contestQueueRepository.popFourUsers(lang.getValue(), tier);

            if (userIds != null && userIds.size() == 4) {
                log.info("매칭 성공! Lang: {}, Tier: {}, Users: {}", lang.getValue(), tier, userIds);

                // 3. Initialize Match
                String matchId = UUID.randomUUID().toString();
                String difficulty = mapTierToDifficulty(tier);

                List<String> userList = new ArrayList<>(userIds);

                MatchInitDto initDto = MatchInitDto.builder()
                        .matchId(matchId)
                        .userIds(userList)
                        .lang(lang)
                        .time(60) // Design time default?
                        .mode("CONTEST") // Use constant if avail
                        .diff(difficulty)
                        .algo(null) // Not selected yet
                        .build();

                matchService.initializeMatch(initDto);

                // 늦게 연결된 유저를 위한 매치 ID 캐싱
                for (String userIdStr : userList) {
                    try {
                        Long id = Long.parseLong(userIdStr);
                        contestQueueRepository.saveUserMatch(id, matchId, Duration.ofMinutes(5));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid userId format while caching match: {}", userIdStr);
                    }
                }

                // 4. Publish Event
                eventPublisher.publishEvent(new ContestMatchFoundEvent(userList, matchId));
            }
        }
    }

    public String consumePendingMatch(Long userId) {
        String matchId = contestQueueRepository.getUserMatch(userId);
        if (matchId != null) {
            contestQueueRepository.removeUserMatch(userId);
        }
        return matchId;
    }
}
