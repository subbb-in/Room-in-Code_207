package com.hamcheese.roomincode.domain.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

//RRR
@Repository
@RequiredArgsConstructor
public class RankingRedisRepository {
    private static final String RANKING_KEY = "ranking";
    private final RedisTemplate<String, String> redisTemplate;

    //Redis Sorted Set(ZSet)에 랭킹용 점수 반영
    public void updateUserScore(String userId, int xp) {
        redisTemplate.opsForZSet().add(RANKING_KEY, userId, xp);
    }

    //범위로 랭커 조회
    public Set<ZSetOperations.TypedTuple<String>> getRankersByRange(long start, long stop) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, start, stop);
    }

    //전체 랭커 수 조회
    public Long getTotalRankerCount() {
        return redisTemplate.opsForZSet().zCard(RANKING_KEY);
    }

    /**
     *
     * @param userId
     * @return userId에 해당하는 유저의 랭크
     */
    public Long getUserRank(Long userId) {
        return redisTemplate.opsForZSet().reverseRank(RANKING_KEY, userId.toString());
    }
}
