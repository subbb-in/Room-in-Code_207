package com.hamcheese.roomincode.domain.match.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ContestQueueRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // Queue Key: queue:{lang}:{tier}
    // We use ZSet to keep order (score = timestamp)
    private String getQueueKey(String lang, String tier) {
        return "queue:" + lang + ":" + tier;
    }

    private String userMatchKey(Long userId) {
        return "contest:match:" + userId;
    }

    public void addToQueue(String lang, String tier, Long userId) {
        String key = getQueueKey(lang, tier);
        redisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
    }

    public void removeFromQueue(String lang, String tier, Long userId) {
        String key = getQueueKey(lang, tier);
        redisTemplate.opsForZSet().remove(key, userId.toString());
    }

    public Long getQueueSize(String lang, String tier) {
        String key = getQueueKey(lang, tier);
        return redisTemplate.opsForZSet().zCard(key);
    }

    public Set<String> popFourUsers(String lang, String tier) {
        String key = getQueueKey(lang, tier);
        // Get top 4
        Set<String> users = redisTemplate.opsForZSet().range(key, 0, 3);

        if (users != null && users.size() == 4) {
            // Remove them from queue
            redisTemplate.opsForZSet().remove(key, users.toArray());
            return users;
        }
        return null;
    }

    public void saveUserMatch(Long userId, String matchId, Duration ttl) {
        redisTemplate.opsForValue().set(userMatchKey(userId), matchId, ttl);
    }

    public String getUserMatch(Long userId) {
        return redisTemplate.opsForValue().get(userMatchKey(userId));
    }

    public void removeUserMatch(Long userId) {
        redisTemplate.delete(userMatchKey(userId));
    }

    // 유저가 어느 큐에 있는지 찾기 위해 (Disconnect 시)
    // 하지만 Redis에서 Key Pattern Scan은 느리므로, Session에 lang/tier 정보를 저장하는게 낫다.
    // 여기서는 제공하지 않음.
}
