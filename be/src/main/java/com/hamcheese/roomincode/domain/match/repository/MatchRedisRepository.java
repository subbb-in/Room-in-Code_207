package com.hamcheese.roomincode.domain.match.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class MatchRedisRepository {
    private final StringRedisTemplate redisTemplate;

    // ====== Key 생성 헬퍼 ======
    private String metaKey(String matchId) {
        return "match:" + matchId + ":meta";
    }

    private String usersKey(String matchId) {
        return "match:" + matchId + ":users";
    }

    private String turnKey(String matchId) {
        return "match:" + matchId + ":turn";
    }

    private String votesKey(String matchId) {
        return "match:" + matchId + ":votes";
    }

    private String problemKey(Long problemId) {
        return "cache:problem:" + problemId;
    }

    private String hintKey(Long problemId, Long hintId) {
        return "cache:problem:" + problemId + ":hint:" + hintId;
    }

    private String codeKey(String matchId) { return  "match:" + matchId +":code";}

    // ====== 매치 메타정보 ======
    public void saveMatchMeta(String matchId, Map<String, String> meta) {
        redisTemplate.opsForHash().putAll(metaKey(matchId), meta);
    }

    public Map<Object, Object> getMatchMeta(String matchId) {
        return redisTemplate.opsForHash().entries(metaKey(matchId));
    }

    public void updateMatchStat(String matchId, String stat) {
        redisTemplate.opsForHash().put(metaKey(matchId), "stat", stat);
    }

    /**
     * 매치 메타데이터의 특정 필드만 업데이트
     */
    public void updateMatchMetaField(String matchId, String field, String value) {
        redisTemplate.opsForHash().put(metaKey(matchId), field, value);
    }

    /**
     * 매치 메타데이터 여러 필드를 한번에 업데이트 (문제 할당 시 사용)
     */
    public void updateMatchMetaFields(String matchId, Map<String, String> fields) {
        redisTemplate.opsForHash().putAll(metaKey(matchId), fields);
    }

    // ====== 매치 참여자 ======
    public void saveMatchUsers(String matchId, List<String> userIds) {
        String[] users = userIds.toArray(new String[0]);
        redisTemplate.opsForSet().add(usersKey(matchId), users);
    }

    public Set<String> getMatchUsers(String matchId) {
        return redisTemplate.opsForSet().members(usersKey(matchId));
    }

    // 게임에서 유저 삭제
    public void removeMatchUser(String matchId, String userId) {
        redisTemplate.opsForSet().remove(usersKey(matchId), userId);
    }

    // 게임 속 유저수
    public Long countMatchUsers(String matchId) {
        return redisTemplate.opsForSet().size(usersKey(matchId));
    }

    // ====== 문제/힌트 캐싱 ======
    public void saveProblemCache(Long problemId, String problemJson) {
        redisTemplate.opsForValue().set(problemKey(problemId), problemJson);
    }

    // 문제 캐시 조회
    public String getProblemCache(Long problemId) {
        return redisTemplate.opsForValue().get(problemKey(problemId));
    }

    // 힌트 정보 캐싱
    public void saveHintCache(Long problemId, int sequence, String content) {
        String key = "cache:problem:" + problemId + ":hint:" + sequence;
        redisTemplate.opsForValue().set(key, content);
    }

    // ====== 턴 정보 ======
    public void updateTurnInfo(String matchId, String userId, long endAt, int seq) {
        Map<String, String> turnData = new HashMap<>();
        turnData.put("user_id", userId);
        turnData.put("end_at", String.valueOf(endAt));
        turnData.put("seq", String.valueOf(seq));
        redisTemplate.opsForHash().putAll(turnKey(matchId), turnData);
    }

    /**
     * 현재 매치의 턴 정보를 가져옴 (user_id, end_at, seq 등)
     */
    public Map<Object, Object> getTurnInfo(String matchId) {
        return redisTemplate.opsForHash().entries(turnKey(matchId));
    }

    /**
     * 현재 턴인 유저 ID만 바로 조회
     */
    public String getCurrentTurnUserId(String matchId) {
        Object userId = redisTemplate.opsForHash().get(turnKey(matchId), "user_id");
        return userId != null ? userId.toString() : null;
    }

    // 매치 메타 정보에서 특정 필드(mode, problem_id 등) 정보 가져오기
    public String getMatchMetaValue(String matchId, String field) {
        String key = "match:" + matchId + ":meta";
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    // ====== 힌트 사용 관련 로직 ======
    private String viewedHintsKey(String matchId, String userId) {
        return "match:" + matchId + ":user:" + userId + ":viewed_hints";
    }

    // 캐싱된 힌트 조회
    public String getHint(Long problemId, int sequence) {
        String key = "cache:problem:" + problemId + ":hint:" + sequence;
        return redisTemplate.opsForValue().get(key);
    }

    // 힌트 사용 여부
    public boolean isHintViewed(String matchId, String userId, int sequence) {
        return Boolean.TRUE
                .equals(redisTemplate.opsForSet().isMember(viewedHintsKey(matchId, userId), String.valueOf(sequence)));
    }

    // 힌트 사용(열람) 기록 저장
    public void markHintAsViewed(String matchId, String userId, int sequence) {
        redisTemplate.opsForSet().add(viewedHintsKey(matchId, userId), String.valueOf(sequence));
    }

    // ====== 투표 관련 로직 ======
    public void saveAlgorithmCandidates(String matchId, List<String> candidates) {
        String candidateStr = String.join(",", candidates);
        updateMatchMetaField(matchId, "candidates", candidateStr);
    }

    public List<String> getAlgorithmCandidates(String matchId) {
        String candidateStr = getMatchMetaValue(matchId, "candidates");
        if (candidateStr == null || candidateStr.isEmpty()) {
            return List.of();
        }
        return List.of(candidateStr.split(","));
    }

    public Long addVote(String matchId, String userId, String algo) {
        redisTemplate.opsForHash().put(votesKey(matchId), userId, algo);
        return redisTemplate.opsForHash().size(votesKey(matchId));
    }

    public Long getTotalVotes(String matchId) {
        return redisTemplate.opsForHash().size(votesKey(matchId));
    }

    public String getMostVotedAlgorithm(String matchId) {
        Map<Object, Object> votes = redisTemplate.opsForHash().entries(votesKey(matchId));
        Map<String, Integer> counts = new HashMap<>();

        for (Object val : votes.values()) {
            String algo = (String) val;
            counts.put(algo, counts.getOrDefault(algo, 0) + 1);
        }

        // Find max
        int maxVotes = 0;
        for (int count : counts.values()) {
            if (count > maxVotes)
                maxVotes = count;
        }

        List<String> topCandidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == maxVotes) {
                topCandidates.add(entry.getKey());
            }
        }

        if (topCandidates.isEmpty())
            return null;
        if (topCandidates.size() == 1)
            return topCandidates.get(0);

        // 투표결과 무승부면 랜덤으로 뽑음
        Collections.shuffle(topCandidates);
        return topCandidates.get(0);
    }

    // ====== 유저 HP & XP 관리 ======
    private String userHpKey(String matchId, String userId) {
        return "match:" + matchId + ":user:" + userId + ":hp";
    }

    private String userXpKey(String matchId, String userId) {
        return "match:" + matchId + ":user:" + userId + ":xp";
    }

    // 매치 시작 시 유저 HP 초기 설정
    public void setUserHp(String matchId, String userId, int hp) {
        redisTemplate.opsForValue().set(userHpKey(matchId, userId), String.valueOf(hp));
    }

    // 유저 HP 조회
    public int getUserHp(String matchId, String userId) {
        String hp = redisTemplate.opsForValue().get(userHpKey(matchId, userId));
        return hp != null ? Integer.parseInt(hp) : 0;
    }

    // 유저 HP 차감 및 현재 값 반환
    public int decrementUserHp(String matchId, String userId, int penalty) {
        Long updatedHp = redisTemplate.opsForValue().decrement(userHpKey(matchId, userId), penalty);
        return updatedHp != null ? updatedHp.intValue() : 0;
    }

    // 유저 XP 증가 및 현재 값 반환
    public int incrementUserXp(String matchId, String userId, int amount) {
        Long updatedXp = redisTemplate.opsForValue().increment(userXpKey(matchId, userId), amount);
        return updatedXp != null ? updatedXp.intValue() : 0;
    }

    // ====== OpenVidu 토큰 관리 ======
    private String openViduTokensKey(String matchId) {
        return "match:" + matchId + ":openvidu:tokens";
    }

    // OpenVidu 토큰 일괄 저장 (userId -> token)
    public void saveOpenViduTokens(String matchId, Map<String, String> userTokens) {
        redisTemplate.opsForHash().putAll(openViduTokensKey(matchId), userTokens);
    }

    // 특정 유저의 OpenVidu 토큰 조회
    public String getOpenViduToken(String matchId, String userId) {
        Object token = redisTemplate.opsForHash().get(openViduTokensKey(matchId), userId);
        return token != null ? token.toString() : null;
    }

    // OpenVidu 세션 ID 저장
    public void saveOpenViduSessionId(String matchId, String sessionId) {
        redisTemplate.opsForHash().put(metaKey(matchId), "openvidu_session_id", sessionId);
    }

    // OpenVidu 세션 ID 조회
    public String getOpenViduSessionId(String matchId) {
        Object sessionId = redisTemplate.opsForHash().get(metaKey(matchId), "openvidu_session_id");
        return sessionId != null ? sessionId.toString() : null;
    }

    // ====== 채점 서버 제출 위한 로직 ======
    // 총 테스트케이스 개수 저장
    public void saveTestCaseRequirement(String matchId, String userId, int totalCount) {
        String key = "judge:req:" + matchId + ":" + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(totalCount)); // TTL 설정 필요시 추가
    }

    // 현재 통과한 개수 초기화 (0으로)
    public void initPassCount(String matchId, String userId) {
        String key = "judge:pass:" + matchId + ":" + userId;
        redisTemplate.delete(key); // 기존 값 제거
    }

    // 통과 개수 1 증가시키고, 현재 값 반환
    public Long incrementPassCount(String matchId, String userId) {
        String key = "judge:pass:" + matchId + ":" + userId;
        return redisTemplate.opsForValue().increment(key);
    }

    // 총 개수 가져오기
    public Integer getTestCaseRequirement(String matchId, String userId) {
        String key = "judge:req:" + matchId + ":" + userId;
        String val = (String) redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }



    // 게임 종료 시각 기록
    public void setMatchEndTime(String matchId, String endTime) {
        redisTemplate.opsForHash().put(metaKey(matchId), "end_time", endTime);
    }

    // ====== AI 분석용 제출 데이터 저장 (Source Code, Language) ======
    private String submissionKey(String matchId, String userId) {
        return "match:" + matchId + ":submission:" + userId;
    }

    public void saveUserSubmission(String matchId, String userId, String sourceCode, String language) {
        Map<String, String> submissionData = new HashMap<>();
        submissionData.put("sourceCode", sourceCode);
        submissionData.put("language", language);
        redisTemplate.opsForHash().putAll(submissionKey(matchId, userId), submissionData);
    }

    public Map<Object, Object> getUserSubmission(String matchId, String userId) {
        return redisTemplate.opsForHash().entries(submissionKey(matchId, userId));
    }

    // 게임 종료 시각 기록
    public void setMatchEndTime(String matchId, long endTime) {
        redisTemplate.opsForHash().put(metaKey(matchId), "end_time", String.valueOf(endTime));
    }

    // ====== 번역된 문제 관리 ======
    // 번역된 문제 저장
    public void saveTranslatedProblem(String matchId, String lang, String translatedText) {
        String key = "match:" + matchId + ":problem:translated:" + lang;
        redisTemplate.opsForValue().set(key, translatedText);
    }

    // 번역된 문제 조회
    public String getTranslatedProblem(String matchId, String lang) {
        String key = "match:" + matchId + ":problem:translated:" + lang;
        return redisTemplate.opsForValue().get(key);
    }

    // ====== AI 관련 로직 ======
    // 코드 저장
    public void saveSharedCode(String matchId, String code) {
        redisTemplate.opsForValue().set(codeKey(matchId), code);
    }

    // 현재 공용 코드 조회
    public String getSharedCode(String matchId) {
        String code = redisTemplate.opsForValue().get(codeKey(matchId));
        return code != null ? code : ""; // 없으면 빈 문자열
    }

    // 턴 주인 변경 (현재 누구 차례인지 기록)
    public void updateTurnOwner(String matchId, String userId) {
        redisTemplate.opsForHash().put(turnKey(matchId), "user_id", userId);
    }

    // AI 턴 처리 여부 설정 (Atomic Lock)
    public boolean setAiTurnProcessed(String matchId, int sequence) {
        String key = "match:" + matchId + ":ai:processed:" + sequence;
        // 키가 없으면 세팅하고 true 반환, 있으면 false 반환. (1분 후 만료)
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "Y", Duration.ofMinutes(1));
        return result != null && result;
    }

    // ====== 채점 결과 집계 로직 ======

    // 1. 개별 테스트 케이스 결과 저장 (Redis List 사용)
    public void saveIndividualResult(String matchId, String userId, String resultJson) {
        String key = "judge:results:" + matchId + ":" + userId;
        redisTemplate.opsForList().rightPush(key, resultJson);
    }

    // 2. 저장된 모든 개별 결과 리스트 조회
    public List<String> getAllResults(String matchId, String userId) {
        String key = "judge:results:" + matchId + ":" + userId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // 3. 채점 완료 후 관련 임시 데이터 삭제
    public void clearJudgeResults(String matchId, String userId) {
        redisTemplate.delete("judge:results:" + matchId + ":" + userId);
        redisTemplate.delete("judge:req:" + matchId + ":" + userId);
        redisTemplate.delete("judge:pass:" + matchId + ":" + userId);
    }
}
