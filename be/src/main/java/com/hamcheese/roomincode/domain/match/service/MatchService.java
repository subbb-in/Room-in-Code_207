package com.hamcheese.roomincode.domain.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.domain.MatchXpPolicy;
import com.hamcheese.roomincode.domain.match.dto.*;
import com.hamcheese.roomincode.domain.match.event.AiCodeUpdateEvent;
import com.hamcheese.roomincode.domain.match.event.AiTurnStartEvent;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.problem.dao.ProblemDao;
import com.hamcheese.roomincode.domain.problem.dto.Problem;
import com.hamcheese.roomincode.domain.problem.dto.ProblemHint;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.service.UserService;
import com.hamcheese.roomincode.global.infra.openvidu.OpenViduService;
import com.hamcheese.roomincode.global.infra.papago.PapagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRedisRepository matchRedisRepository;
    private final ProblemDao problemDao;
    private final ObjectMapper objectMapper;
    private final OpenViduService openViduService;
    private final MatchTimerService matchTimerService;
    private final AiClientService aiClientService;
    private final ApplicationEventPublisher eventPublisher; // 웹소켓 전송용
    private final UserDao userDao;
    private final UserService userService;

    // 상태 및 모드 상수화
    private static final String STAT_VOTING = "VOTING";
    private static final String STAT_DESIGN = "DESIGN";
    private static final String MODE_CONTEST = "CONTEST";
    private static final String MODE_FRIENDS = "FRIENDS";
    private static final String MODE_AI = "AI";
    private static final String AI_BOT = "AI_BOT"; // 가상의 AI 유저 ID
    private final PapagoService papagoService;

    // 8 Algorithm Types
    private static final List<String> ALGORITHMS = Arrays.asList(
            "Amortized analysis", "Bit manipulation", "Complete search", "Data structures",
            "Dynamic programming", "Greedy algorithms", "Range queries", "Sorting");

    /**
     * 1. 매치 초기화 (공통 진입점)
     */
    @Transactional(readOnly = true)
    public void initializeMatch(MatchInitDto initDto) {
        String matchId = initDto.getMatchId();
        log.info("매치 초기화 시작 - ID: {}, Mode: {}", matchId, initDto.getMode());

        // A. 기본 메타데이터 저장 (상태 설정 포함)
        setupBaseMeta(initDto);

        // B. 유저 초기화 (HP 설정 포함)
        if(MODE_AI.equals(initDto.getMode())) {
            // 유저 리스트에 실제 유저와 AI 봇 추가
            List<String> participants = new ArrayList<>(initDto.getUserIds());
            participants.add(AI_BOT);
            setupParticipants(matchId, participants);
        } else {
            setupParticipants(matchId, initDto.getUserIds());
        }

        // D. 모드별 분기 처리
        if(MODE_AI.equals(initDto.getMode())) {
            // AI 모드는 OpenVidu 세션 생성 x
            assignProblemToMatch(matchId, initDto.getAlgo(), initDto.getDiff());
            matchRedisRepository.updateMatchStat(matchId, STAT_DESIGN);
        } else if (MODE_FRIENDS.equals(initDto.getMode())) {
            // 친구 모드는 즉시 문제 할당 및 DESIGN 단계 진입
            assignProblemToMatch(matchId, initDto.getAlgo(), initDto.getDiff());
            matchRedisRepository.updateMatchStat(matchId, STAT_DESIGN);

            // OpenVidu 세션 생성
//            setupMediaSession(matchId, initDto.getUserIds());
        } else if (MODE_CONTEST.equals(initDto.getMode())) {
            // 컨테스트 모드는 VOTING 상태로 대기
            // 1. Random 2 Algorithms selection
            startVotingPhase(matchId);
        } else {
            log.warn("Match Mode가 유효하지 않습니다: {}", initDto.getMode());
        }
    }

    /**
     * 투표 페이즈 시작
     */
    private void startVotingPhase(String matchId) {
        log.info("매치[{}] 투표 페이즈 시작", matchId);

        // 1. 2개의 랜덤 알고리즘 고름
        List<String> candidates = new ArrayList<>(ALGORITHMS);
        Collections.shuffle(candidates);
        List<String> selected = candidates.subList(0, 2);

        // 2. 2개 레디스에 저장
        matchRedisRepository.saveAlgorithmCandidates(matchId, selected);

        // 3. Broadcast VOTING_START
    }

    /**
     * Process User Vote
     */
    @Transactional
    public void processVote(String matchId, String userId, String votedAlgo) {
        // 1. Verify Vote Phase
        String stat = matchRedisRepository.getMatchMetaValue(matchId, "stat");
        if (!STAT_VOTING.equals(stat)) {
            throw new IllegalStateException("Not in VOTING phase");
        }

        // 2. Validate Algo is candidate
        List<String> candidates = matchRedisRepository.getAlgorithmCandidates(matchId);
        if (!candidates.contains(votedAlgo)) {
            throw new IllegalArgumentException("Invalid Algorithm candidate");
        }

        // 3. Save Vote
        Long voteCount = matchRedisRepository.addVote(matchId, userId, votedAlgo);

        // 4. Check if all voted?
        Long userCount = Long.parseLong(matchRedisRepository.getMatchMetaValue(matchId, "cnt"));
        Long totalVotes = matchRedisRepository.getTotalVotes(matchId);

        if (totalVotes >= userCount) {
            finishVoting(matchId);
        }
    }

    private void finishVoting(String matchId) {
        // 1. Count votes
        String winner = matchRedisRepository.getMostVotedAlgorithm(matchId);

        // 2. Assign Problem
        String diff = matchRedisRepository.getMatchMetaValue(matchId, "diff");
        // Difficulty mapping is already done in ContestService and passed as `diff`
        // (e.g. VERY_HARD)
        // Wait, MatchInitDto has `diff`.

        assignProblemAfterVote(matchId, winner, diff);

        // 3. Broadcast Result (Need Event or Handler access)
    }

    @Transactional
    public void assignProblemAfterVote(String matchId, String algo, String diff) {
        log.info("매치[{}] 투표 완료 결과 반영 - Algo: {}, Diff: {}", matchId, algo, diff);

        // 문제 할당 로직 수행
        assignProblemToMatch(matchId, algo, diff);

        // 상태를 DESIGN으로 변경하여 설계 단계 시작 브로드캐스트 준비
        matchRedisRepository.updateMatchStat(matchId, STAT_DESIGN);
    }

    /**
     * 문제를 DB에서 조회하여 Redis에 캐싱하고 매치에 연결 (내부 공통 로직)
     */
    private void assignProblemToMatch(String matchId, String algo, String diff) {
        Problem problem = problemDao.findRandomProblem(algo, diff);
        if (problem == null)
            throw new IllegalStateException("조건에 맞는 문제가 없습니다.");

        try {
            // 1. 문제 정보 Redis 캐싱
            matchRedisRepository.saveProblemCache(problem.getProblemId(), objectMapper.writeValueAsString(problem));

            // 2. 힌트 정보 Redis 캐싱
            List<ProblemHint> hints = problemDao.findHintsByProblemId(problem.getProblemId());
            for (ProblemHint hint : hints) {
                matchRedisRepository.saveHintCache(problem.getProblemId(), hint.getSequence(), hint.getContent());
            }

            // 번역 데이터
            String problemJson = matchRedisRepository.getProblemCache(problem.getProblemId());

            // 비동기 요청을 동기적으로 기다림
            String translatedProblem = papagoService.translate(problem.getQuestion(), "en", "ko")
                    .doOnSuccess(res -> log.info("매치[{}] 문제 한국어 번역 완료", matchId))
                    .doOnError(e -> log.error("매치[{}] 문제 번역 실패: {}", matchId, e.getMessage()))
                    .block(Duration.ofSeconds(5)); // 최대 5초 대기

            // 번역본 저장
            matchRedisRepository.saveTranslatedProblem(matchId, "ko", translatedProblem);

            // 3. 매치 메타데이터 업데이트 (문제 ID 연동)
            Map<String, String> updates = new HashMap<>();
            updates.put("problem_id", String.valueOf(problem.getProblemId()));
            updates.put("algo", algo);
            updates.put("diff", diff);
            matchRedisRepository.updateMatchMetaFields(matchId, updates);

            log.info("매치[{}] 문제 할당 완료: ProblemID {}", matchId, problem.getProblemId());
        } catch (Exception e) {
            log.error("문제 할당 중 캐싱 오류: {}", e.getMessage());
            throw new RuntimeException("문제 할당 실패", e);
        }
    }

    // --- Private Helper Methods (코드 분리) ---

    private void setupBaseMeta(MatchInitDto initDto) {
        Map<String, String> meta = new HashMap<>();
        int userCount = (initDto.getUserIds() != null) ? initDto.getUserIds().size() : 0;
        meta.put("cnt", String.valueOf(userCount));
        meta.put("lang", initDto.getLang().getValue());
        meta.put("mode", initDto.getMode());
        meta.put("time", String.valueOf(initDto.getTime()));
        meta.put("start_time", String.valueOf(System.currentTimeMillis()));

        // 초기 상태 결정
        String initialStat = MODE_FRIENDS.equals(initDto.getMode()) ? STAT_DESIGN : STAT_VOTING;
        meta.put("stat", initialStat);

        matchRedisRepository.saveMatchMeta(initDto.getMatchId(), meta);
    }

    // 플레이어 셋업
    private void setupParticipants(String matchId, List<String> userIds) {
        if (userIds != null){
            matchRedisRepository.saveMatchUsers(matchId, userIds);
            for (String userId : userIds) {
                if(userId.equals(AI_BOT)) {
                    // AI 모드에서는 hp 무한대
                    matchRedisRepository.setUserHp(matchId, userId, 9999);
                } else { // 다른 두 모드에서는 hp 100으로 시작
                    matchRedisRepository.setUserHp(matchId, userId, 100);
                }
            }
        }
    }

    // OpenVidu 연결
    private void setupMediaSession(String matchId, List<String> userIds) {
        try {
            String openViduSessionId = openViduService.createSessionWithId(matchId);
            matchRedisRepository.saveOpenViduSessionId(matchId, openViduSessionId);

            if (userIds != null && !userIds.isEmpty()) {
                Map<String, String> userTokens = new HashMap<>();
                for (String userId : userIds) {
                    userTokens.put(userId, openViduService.generateToken(openViduSessionId));
                }
                matchRedisRepository.saveOpenViduTokens(matchId, userTokens);
            }
        } catch (Exception e) {
            log.error("OpenVidu 세션 생성 실패: {}", e.getMessage());
        }
    }

    // 힌트 사용
    public HintResponseDto useHint(HintRequestDto hintRequestDto) {
        String matchId = hintRequestDto.getMatchId();
        String userId = hintRequestDto.getUserId();
        int sequence = hintRequestDto.getSequence();

        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId가 없습니다");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId가 없습니다");
        }

        // 1. 이미 확인한 힌트인지 체크 (중복 차감 방지)
        boolean alreadyViewed = matchRedisRepository.isHintViewed(matchId, userId, sequence);

        int currentHp = matchRedisRepository.getUserHp(matchId, userId);
        String problemIdStr = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
        if (problemIdStr == null || problemIdStr.isBlank()) {
            throw new IllegalStateException("problem_id가 설정되지 않았습니다");
        }
        Long problemId = Long.parseLong(problemIdStr);
        String hintContent = matchRedisRepository.getHint(problemId, sequence);

        if (alreadyViewed) {
            // 이미 본 힌트라면 HP 차감 없이 정보만 반환
            return new HintResponseDto("HINT_RESPONSE", matchId, userId, sequence, hintContent, currentHp);
        }

        // 2. 현재 hp 확인하고 10보다 적으면 힌트 확인 불가
        if (currentHp < 10) {
            throw new IllegalStateException("힌트를 볼 수 있는 hp가 부족합니다");
        }

        // 3. HP 차감 및 열람 기록 저장
        int updatedHp = matchRedisRepository.decrementUserHp(matchId, userId, 10);
        matchRedisRepository.markHintAsViewed(matchId, userId, sequence);

        return new HintResponseDto("HINT_RESPONSE", matchId, userId, sequence, hintContent, updatedHp);
    }

    /**
     * 매치가 시작 가능한지 확인하고 초기화 정보를 반환
     */
    public Map<Object, Object> attemptStartMatch(String matchId, int currentConnectedCount) {
        Map<Object, Object> meta = matchRedisRepository.getMatchMeta(matchId);
        if (meta == null || meta.isEmpty())
            return null;

        int totalCnt = Integer.parseInt((String) meta.get("cnt"));
        String stat = (String) meta.get("stat");
        String mode = (String) meta.get("mode");

        // (AI 모드는 실제 세션은 1개 유저뿐인데 정원이 2명이어서 시작 안됨)
        // AI 모드라면 세션 1개만 연결되어도 시작 가능하도록 허용
        if(MODE_AI.equals(mode) && currentConnectedCount >=1) {
            return meta;
        }

        // [수정 핵심] VOTING 단계일 때만 '전원 접속'을 엄격하게 체크
        if (STAT_VOTING.equals(stat)) {
            if (currentConnectedCount >= totalCnt) {
                return meta;
            }
            return null; // 아직 다 안 모임
        }
        // [수정 핵심] DESIGN이나 PLAYING 단계라면, 이미 게임이 진행 중이므로
        // 접속 인원수와 관계없이 정보를 반환해서 재접속 처리를 돕는다.
        if (STAT_DESIGN.equals(stat) || "PLAYING".equals(stat)) {
            return meta;
        }

        // 그 외의 경우 (아직 시작 전인데 인원 부족 등)
        if (currentConnectedCount >= totalCnt) {
            return meta;
        }

        return null;
    }

    public List<String> getAlgorithmCandidates(String matchId) {
        return matchRedisRepository.getAlgorithmCandidates(matchId);
    }

    /**
     * 설계 종료 후 게임 시작 로직 (Handler에서 호출)
     */
    public String startPlayingPhase(String matchId) {
        log.info("매치[{}] 설계 종료. 턴제 코딩(PLAYING) 시작", matchId);
        matchRedisRepository.updateMatchStat(matchId, "PLAYING");

        // 1. 참여자 조회
        Set<String> users = matchRedisRepository.getMatchUsers(matchId);

        if (users.isEmpty()) {
            throw new IllegalStateException("참여자가 없습니다.");
        }

        // 첫 번째 유저 선정 (Redis Set의 첫 번째 멤버)
        // + AI 모드인 경우, 실제 유저가 선공하도록 처리
        String firstUser;
        if(users.contains(AI_BOT)) {
            firstUser = users.stream()
                    .filter(u -> !u.equals(AI_BOT))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("실제 유저가 없습니다."));
        } else {
            // UserId 사전순 정렬
            List<String> sortedUsers = new ArrayList<>(users);
            Collections.sort(sortedUsers);
            firstUser = sortedUsers.get(0);
        }

        int startSeq = 0;

        // 턴 정보 업데이트 (Redis)
        //matchRedisRepository.updateTurnInfo(matchId, firstUser, System.currentTimeMillis() + 30000, 1);
        // 2. 타이머 서비스 시작 (Redis 업데이트 + 스케줄링 + 브로드캐스팅 모두 수행)
        matchTimerService.startTurn(matchId, firstUser, startSeq);

        return firstUser; // 첫 번째 턴 유저 ID 반환
    }

    /**
     * AI 턴 시작 이벤트 리스너
     */
    @EventListener
    @Async // 비동기로 실행되어야 함
    public void handleAiTurnStart(AiTurnStartEvent event) {
        processAiTurn(event.getMatchId(), event.getSequence());
    }

    /**
     * AI 로직 수행 -> 끝나면 턴 넘기기
     */
    public void processAiTurn(String matchId, int sequence) {
        log.info("AI 턴 로직 실행: {}", matchId);

        // 1. 중복 실행 방지 Check (Redis Atomic Lock)
        if (!matchRedisRepository.setAiTurnProcessed(matchId, sequence)) {
            log.warn("매치[{}] 시퀀스[{}] AI 턴 이미 처리됨. 중복 실행 방지", matchId, sequence);
            return;
        }

        try {
            // 2. 데이터 조회
            String sharedCode = matchRedisRepository.getSharedCode(matchId);
            Problem problem = getMatchProblem(matchId);
            String lang = matchRedisRepository.getMatchMetaValue(matchId, "lang");

            // 3. AI 서버 호출
            String newCode = aiClientService.requestAiRelay(problem.getQuestion(), sharedCode, lang);

            // 4. Redis 저장 (코드 유효할 때만)
            if(newCode != null && !newCode.isEmpty()) {
                matchRedisRepository.saveSharedCode(matchId, newCode);

                // 5. 이벤트 발행 - 웹소켓으로 코드 변경 알림 (EventListener가 처리)
                eventPublisher.publishEvent(AiCodeUpdateEvent.builder()
                        .matchId(matchId)
                        .newCode(newCode)
                        .build());
            }

            log.info("AI 로직 완료. 턴 시간({}s) 동안 대기 (자동 넘김 아님)", 
                    matchRedisRepository.getMatchMetaValue(matchId, "time"));
            
            // [변경] AI는 턴 시간을 모두 사용해야 하므로 여기서 rotateTurn을 호출하지 않음.
            // TimerService가 시간 종료 시 자동으로 턴을 넘겨줌.

        } catch (Exception e) {
            log.error("AI 턴 처리 중 오류", e);
            // 오류 발생 시에도 턴은 강제로 넘기지 않고 타이머에 맡김 (혹은 필요 시 재시도 로직?)
            // 현재는 그냥 로그만 찍고 유지
        }
    }

    /**
     * AI 모드에서 유저가 제출 또는 턴 넘기기
     */
    public void finishUserTurn(String matchId, String userId) {
        // [추가] 턴 주인 검증
        String currentTurnUserId = matchRedisRepository.getCurrentTurnUserId(matchId);
        if (currentTurnUserId == null || !currentTurnUserId.equals(userId)) {
            log.warn("매치[{}] 유저[{}] 턴 종료 시도 실패 - 현재 턴 주인: {}", matchId, userId, currentTurnUserId);
            return; // 권한 없음 -> 무시
        }

        // 현재 시퀀스 정보 가져오기
        Map<Object, Object> turnInfo = matchRedisRepository.getTurnInfo(matchId);
        int currentSeq = Integer.parseInt((String) turnInfo.getOrDefault("seq", "0"));

        // TimerService를 통해 턴 넘기기
        matchTimerService.rotateTurn(matchId, userId, currentSeq);
    }

    /**
     * 현재 유저가 편집 권한이 있는지 확인
     * 프론트에서도 막아줘야
     * TODO: 실제 테스트 때 코드 동기화 지연 발생한다면 현재 턴 유저 캐싱 필요
     */
    public boolean canUserEdit(String matchId, String userId) {
        //1. 매치 정보 확인
        String stat = matchRedisRepository.getMatchMetaValue(matchId, "stat");
        if (!"PLAYING".equals(stat)) {
            log.warn("매치 {}: 현재 PLAYING중이 아님: {}", matchId, stat);
            return false;
        }

        //2. Redis에서 현재 턴 유저 조회
        String currentTurnUserId = matchRedisRepository.getCurrentTurnUserId(matchId);

        //3. 현재 수정요청하는 userId와 턴 유저 비교
        boolean isTurnOwner = userId != null && userId.equals(currentTurnUserId);

        if (!isTurnOwner) {
            log.debug("매치 {} 수정 시도 거부 - 현재 턴: {}, 수정 요청 유저: {}",
                    matchId, currentTurnUserId, userId);
        }

        return isTurnOwner;
    }

    /**
     * 매치에 연결된 문제 정보 반환
     * Redis 캐시 우선, 없으면 DB 조회 후 캐시 저장
     */
    @Transactional(readOnly = true)
    public Problem getMatchProblem(String matchId) {
        String problemIdValue = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
        if (problemIdValue == null) {
            throw new IllegalStateException("매치에 연결된 문제 정보가 없습니다.");
        }

        Long problemId = Long.parseLong(problemIdValue);

        try {
            String cached = matchRedisRepository.getProblemCache(problemId);
            if (cached != null) {
                return objectMapper.readValue(cached, Problem.class);
            }
        } catch (Exception e) {
            log.warn("문제 캐시 파싱 실패: {}", e.getMessage());
        }

        Problem problem = problemDao.findById(problemId);
        if (problem == null) {
            throw new IllegalStateException("문제를 찾을 수 없습니다.");
        }

        try {
            String problemJson = objectMapper.writeValueAsString(problem);
            matchRedisRepository.saveProblemCache(problemId, problemJson);
        } catch (Exception e) {
            log.warn("문제 캐시 저장 실패: {}", e.getMessage());
        }

        return problem;
    }

    /**
     * 번역된 문제 조회
     */
    public String getTranslatedProblem(String matchId, String lang) {
        return matchRedisRepository.getTranslatedProblem(matchId, lang);
    }

    //TODO: 빈 순환참조 - 우선 실행을 위해 주석처리해둠
    /**
     * 채점 성공 시 게임 종료 처리
     * 매치 상태 변경, OpenVidu 세션 정리, 종료 시각 기록
     */
    public void handleGradingSuccess(String matchId) {
        log.info("매치[{}] 채점 성공. 게임 종료 처리 시작", matchId);
        cleanupMatchResources(matchId);
    }

    /**
     *  유저가 게임 나감 (XP 페널티)
     */
    @Transactional
    public void leaveMatch(String matchId, String userId) {
        // 1. 매치 메타 정보 조회
        Map<Object, Object> meta = matchRedisRepository.getMatchMeta(matchId);
        if (meta == null || meta.isEmpty()) {
            log.warn("매치[{}] 메타 정보 없음 - 이미 종료된 매치일 수 있음", matchId);
            return;
        }

        String stat = (String) meta.get("stat");
        String mode = (String) meta.get("mode");

        // 2. 게임 진행 중(PLAYING)이면 XP 페널티 적용
        if ("PLAYING".equals(stat) && mode != null) {
            try {
                MatchXpPolicy policy = MatchXpPolicy.valueOf(mode.toUpperCase());
                int penalty = policy.getPenalty(); // FRIENDS: -50, RANKING: -100, AI: 0

                if (penalty < 0) {
                    userService.updateXp(Long.parseLong(userId), penalty);
                    log.info("매치[{}] 유저[{}] 중도 이탈로 XP {} 차감", matchId, userId, penalty);
                }
            } catch (IllegalArgumentException e) {
                log.warn("매치[{}] 알 수 없는 모드 '{}' - XP 페널티 미적용", matchId, mode);
            } catch (Exception e) {
                log.error("매치[{}] 유저[{}] XP 페널티 적용 중 오류: {}", matchId, userId, e.getMessage());
            }

        }

        // 3. Redis에서 유저 제거
        matchRedisRepository.removeMatchUser(matchId, userId);

        // 4. 남은 인원 확인
        Long remainingCount = matchRedisRepository.countMatchUsers(matchId);
        log.info("매치[{}] 유저[{}] 퇴장, 남은 인원: {}명", matchId, userId, remainingCount);

        // 5. 종료조건 체크
        // (1) 남은 인원이 0명 이하
        // (2) AI 모드라면 유저가 나가면 (남은 인원 1명 - AIBOT) 바로 종료
        boolean isAiMode = "AI".equals(mode);

        if ((remainingCount != null && remainingCount <= 0) || isAiMode) {
            log.info("매치[{}] 모든 인원 퇴장. 게임 종료 처리 시작", matchId);
            cleanupMatchResources(matchId);
        }
    }

    /**
     * 매치 자원 정리
     */
    private void cleanupMatchResources(String matchId) {
        try {
            // 매치 상태 변경
            matchRedisRepository.updateMatchStat(matchId, "FINISHED");

            // OpenVidu 세션 종료
            String sessionId = matchRedisRepository.getOpenViduSessionId(matchId);
            if (sessionId != null) {
                openViduService.closeSession(sessionId);
                log.info("매치[{}] OpenVidu 세션[{}] 종료 완료", matchId, sessionId);
            }

            // 타이머 중지
            matchTimerService.cancelTimer(matchId);

            // 종료 시각 기록
            matchRedisRepository.setMatchEndTime(matchId, String.valueOf(System.currentTimeMillis()));

            log.info("매치[{}] 자원 정리 완료", matchId);

        } catch (Exception e) {
            log.error("매치[{}] 자원 정리 중 오류: {}", matchId, e.getMessage(), e);
        }
    }

    /**
     * 1. 개별 콜백 처리 (Judge0가 각 케이스마다 호출)
     */
    @Transactional
    public Judge0ResultDto.MatchResult handleCallback(String matchId, String userId, Judge0CallbackDto callbackDto) {
        try {
            // 1. 개별 결과 저장
            String resultJson = objectMapper.writeValueAsString(callbackDto);
            matchRedisRepository.saveIndividualResult(matchId, userId, resultJson);

            // 2. 카운트 증가 및 전체 개수 확인
            Long currentCount = matchRedisRepository.incrementPassCount(matchId, userId);
            Integer totalCount = matchRedisRepository.getTestCaseRequirement(matchId, userId);

            // 3. 마지막 콜백인 경우에만 최종 집계 로직 실행
            if (currentCount != null && currentCount.intValue() == totalCount) {
                List<String> allResults = matchRedisRepository.getAllResults(matchId, userId);

                // 최종 집계 메서드 호출 (기존 로직 수행 후 MatchResult 반환)
                Judge0ResultDto.MatchResult finalResult = processFinalJudgeResult(matchId, userId, allResults);

                // 처리 완료 후 청소
                matchRedisRepository.clearJudgeResults(matchId, userId);

                return finalResult; // 마지막이면 결과 반환
            }
        } catch (Exception e) {
            log.error("콜백 처리 중 오류", e);
        }
        return null; // 아직 진행 중이면 null 반환
    }

    /**
     * 2. 최종 집계 및 비즈니스 로직 (모든 케이스 수집 완료 후 1회 실행)
     */
    @Transactional
    public Judge0ResultDto.MatchResult processFinalJudgeResult(String matchId, String userId, List<String> resultJsons) {
        try {
            // 1. String 리스트를 객체 리스트로 변환
            List<Judge0ResultDto.Judge0Response> submissions = resultJsons.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, Judge0ResultDto.Judge0Response.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 2. 전체 통과 여부 및 성능 지표 계산 (Base64 디코딩 고려)
            boolean isAllPassed = submissions.stream()
                    .allMatch(s -> s.getStatus() != null && s.getStatus().getId() == 3);

            // 실행 시간 및 메모리 (가장 높은 부하를 준 케이스 기준)
            double maxTime = submissions.stream()
                    .mapToDouble(s -> s.getTime() != null ? Double.parseDouble(s.getTime()) : 0.0)
                    .max().orElse(0.0);

            int maxMemory = submissions.stream()
                    .mapToInt(s -> s.getMemory() != null ? Math.round(s.getMemory()) : 0)
                    .max().orElse(0);

            // 문제의 제한 사항 정보 가져오기 (Redis 캐시 활용)
            String problemIdStr = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
            Long problemId = Long.parseLong(problemIdStr);

            // 문제 상세 정보 조회 (시간/메모리 제한 반환용)
            Problem problem = problemDao.findById(problemId);

            // 3. 실패 케이스 분석 (첫 번째 실패 지점 추출)
            String failureMessage = "Accepted";
            String displayStdout = "";

            if (!isAllPassed) {
                Judge0ResultDto.Judge0Response firstFail = submissions.stream()
                        .filter(s -> s.getStatus() != null && s.getStatus().getId() != 3)
                        .findFirst()
                        .orElse(submissions.get(0));

                failureMessage = (firstFail.getStatus() != null) ? firstFail.getStatus().getDescription() : "Unknown Error";
                // 사용자에게 보여줄 에러 상세 (디코딩된 값 사용)
                displayStdout = firstFail.getDecodedStdout();
                if (displayStdout.isEmpty()) displayStdout = firstFail.getDecodedMessage();

                // [실패 로직] HP 차감
                log.info("매치[{}] 유저[{}] 채점 실패: {}", matchId, userId, failureMessage);
                matchRedisRepository.decrementUserHp(matchId, userId, 20);
            } else {
                // [성공 로직]
                log.info("매치[{}] 유저[{}] 모든 테스트 케이스 통과", matchId, userId);
                displayStdout = submissions.get(0).getDecodedStdout();

                // 1. 모든 참여자 조회
                Set<String> matchUsers = matchRedisRepository.getMatchUsers(matchId);

                // 2. 모든 참여자에 대해 history 생성 (기존에 없는 경우에만)
                for (String participantId : matchUsers) {
                    Long pId = Long.parseLong(participantId);
                    if (!userDao.existsHistory(pId, problemId)) {
                        List<Long> skillIds = userDao.findSkillIdsByProblemId(problemId);
                        userDao.saveSolvedHistory(pId, problemId, skillIds);
                    }
                }

                // XP 지급 로직
                processXpReward(matchId, userId);

                // AI 분석 요청 호출
                requestAiAnalysis(matchId, userId, problemId, maxTime, maxMemory);
            }

            // 4. 최종 결과 DTO 반환
            return Judge0ResultDto.MatchResult.builder()
                    .success(isAllPassed)
                    .message(failureMessage)
                    .stdout(displayStdout)
                    .timeLimit(problem.getTimeLimit()) // 문제 기준치 포함
                    .memoryLimit(problem.getMemoryLimit())
                    .executedTime(maxTime)
                    .executedMemory(maxMemory)
                    .build();

        } catch (Exception e) {
            log.error("채점 결과 처리 중 오류 발생: matchId={}, userId={}", matchId, userId, e);
            throw new RuntimeException("채점 시스템 내부 오류", e);
        }
    }

    /**
     * XP 보너스 및 기본 XP 정산 로직 분리
     */
    private void processXpReward(String matchId, String userId) {
        String mode = matchRedisRepository.getMatchMetaValue(matchId, "mode");
        String diff = matchRedisRepository.getMatchMetaValue(matchId, "diff");

        MatchXpPolicy xpPolicy = MatchXpPolicy.valueOf(mode.toUpperCase());
        int baseXp = xpPolicy.getXp(diff);

        Set<String> participants = matchRedisRepository.getMatchUsers(matchId);
        for (String pid : participants) {
            int reward = baseXp;
            if (pid.equals(userId)) {
                reward += xpPolicy.getSubmitBonus(); // 제출자 보너스
            }
            matchRedisRepository.incrementUserXp(matchId, pid, reward);
        }
    }

    /**
     * AI 분석 서버 비동기 호출 분리
     */
    private void requestAiAnalysis(String matchId, String userId, Long problemId, double time, int memory) {
        try {
            Map<Object, Object> submission = matchRedisRepository.getUserSubmission(matchId, userId);
            if (submission != null && !submission.isEmpty()) {
                AiRequestDto.AiFeedbackRequestDto request = AiRequestDto.AiFeedbackRequestDto.builder()
                        .userId(userId)
                        .matchId(matchId)
                        .problemId(problemId)
                        .userCode(String.valueOf(submission.get("sourceCode")))
                        .language(String.valueOf(submission.get("language")))
                        .executedTime(time)
                        .executedMemory(memory)
                        .build();
                aiClientService.analyzeMatch(request);
            }
        } catch (Exception e) {
            log.error("AI 분석 요청 실패 (게임은 계속 진행): {}", e.getMessage());
        }
    }

//    // Judge0 결과 처리 (Batch)
//     /* 채점 결과
//     * Judge0ResultDto.Judge0Response에 정의해서 수동으로 파싱할 필요 x
//     */
//    @Transactional
//    public Judge0ResultDto.MatchResult processJudgeResult(String matchId, String userId, String resultJson) {
//        try {
//            // 1. Batch Wrapper로 받기
//            Judge0ResultDto.Judge0BatchResponse batchResponse =
//                    objectMapper.readValue(resultJson, Judge0ResultDto.Judge0BatchResponse.class);
//
//            List<Judge0ResultDto.Judge0Response> submissions = batchResponse.getSubmissions();
//
//            if (submissions == null || submissions.isEmpty()) {
//                return Judge0ResultDto.MatchResult.builder()
//                        .success(false)
//                        .message("응답 데이터가 없습니다.")
//                        .build();
//            }
//
//            // 성공 여부: 하나라도 id가 3(Accepted)이 아니면 실패 처리
//            // 먼저 전체 통과 여부 확인
//            boolean isAllPassed = submissions.stream()
//                    .allMatch(s -> s.getStatus() != null && s.getStatus().getId() == 3);
//
//            // 메모리, 실행시간 계산: 실행 시간/메모리 중 가장 큰 값 (또는 평균) 추출
//            // 여기서는 '가장 느렸던 케이스'를 기준으로 잡음 (Worst Case)
//            double maxTime = submissions.stream()
//                    .mapToDouble(s -> s.getTime() != null ? s.getTime().doubleValue() : 0.0)
//                    .max().orElse(0.0);
//
//            long maxMemory = submissions.stream()
//                    .mapToLong(s -> s.getMemory() != null ? Math.round(s.getMemory()) : 0L)
//                    .max().orElse(0L);
//
//            // 첫 번째 실패 케이스의 메시지 찾기 (실패 시 반환용)
//            String failureMessage = "Wrong Answer";
//            String failureStdout = "";
//            if (!isAllPassed) {
//                Judge0ResultDto.Judge0Response updateFail = submissions.stream()
//                        .filter(s -> s.getStatus() != null && s.getStatus().getId() != 3)
//                        .findFirst()
//                        .orElse(submissions.getFirst());
//
//                if (updateFail.getStatus() != null) {
//                    failureMessage = updateFail.getStatus().getDescription();
//                }
//                failureStdout = updateFail.getStdout();
//            } else {
//                // 성공 시 첫 번째 케이스의 stdout (혹은 비워둠)
//                failureStdout = submissions.getFirst().getStdout();
//            }
//
//            // 4. 실패 시 로직
//            if (!isAllPassed) {
//                log.info("매치[{}] 유저[{}] 채점 실패: {}", matchId, userId, failureMessage);
//                matchRedisRepository.decrementUserHp(matchId, userId, 20); // HP 차감
//
//                return Judge0ResultDto.MatchResult.builder()
//                        .success(false)
//                        .message(failureMessage)
//                        .stdout(failureStdout)
//                        .executedTime(maxTime)
//                        .executedMemory((int) maxMemory)
//                        .build();
//            }
//
//            // 성공 시나리오
//            log.info("매치[{}] 유저[{}] 최종 합격 (SUCCESS)", matchId, userId);
//
//            // 1) 문제 풀이 기록 DB 저장
//            String problemIdStr = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
//
//            Long problemId = Long.parseLong(problemIdStr);
//            Long solvedUserId = Long.parseLong(userId);
//
//            // 처음 푼 문제인지 db에서 확인
//            if(!userDao.existsHistory(solvedUserId, problemId)) {
//                log.info("유저[{}] - 문제[{}] 성공, 히스토리 저장", solvedUserId, problemId);
//                // 문제에 매핑된 모든 스킬id 리스트 조회함
//                List<Long> skillIds = userDao.findSkillIdsByProblemId(problemId);
//
//                if(skillIds != null && !skillIds.isEmpty()) {
//                    // 문제풀이 기록 history 테이블 저장
//                    userDao.saveSolvedHistory(solvedUserId, problemId, skillIds);
//                }
//            } else {
//                log.info("유저[{}] - 문제[{}] 이미 푼 문제라 통계에 합산 x", solvedUserId, problemId);
//            }
//
//            // 2) 레디스에서 모드와 난이도 정보 가져오기
//            String mode = matchRedisRepository.getMatchMetaValue(matchId, "mode" );
//            String diff = matchRedisRepository.getMatchMetaValue(matchId, "diff");
//
//            // 3) xp 가져오기
//            MatchXpPolicy matchXpPolicy = MatchXpPolicy.valueOf(mode.toUpperCase());
//            int baseXp = matchXpPolicy.getXp(diff); // 기본 점수
//
//            // 게임 참여자 목록 조회
//            Set<String> matchUsers = matchRedisRepository.getMatchUsers(matchId);
//
//            for (String participantId : matchUsers) {
//                int finalXp = baseXp; // 각 루프마다 새로 할당
//                // 제출한 유저는 추가 Xp 보너스
//                if (participantId.equals(userId)) {
//                    finalXp += matchXpPolicy.getSubmitBonus();
//                }
//
//                // 참여자 전체 기본 XP 증가
//                matchRedisRepository.incrementUserXp(matchId, participantId, finalXp);
//            }
//
//            // AI 서버 분석 요청
//            try {
//                // Redis에서 제출한 소스코드와 언어 정보 조회
//                Map<Object, Object> submissionData = matchRedisRepository.getUserSubmission(matchId, userId);
//                String problemIdString = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
//
//                if (submissionData != null && !submissionData.isEmpty()) {
//                    String sourceCode = String.valueOf(submissionData.get("sourceCode"));
//                    String language = String.valueOf(submissionData.get("language"));
//
//                    // 분석에 필요한 데이터를 dto에 담음
//                    AiRequestDto analysisRequest = AiRequestDto.builder()
//                            .matchId(matchId)
//                            .userId(userId)
//                            .problemId(problemIdString != null ? Long.parseLong(problemIdString) : null)
//                            .sourceCode(sourceCode)
//                            .language(language)
//                            .executedTime(maxTime)
//                            .executedMemory((int) maxMemory)
//                            .build();
//
//                    // AI 서버를 호출할 Service
//                    aiClientService.analyzeMatch(analysisRequest);
//
//                    log.info("매치[{}] 유저[{}] AI 분석 요청 전송 완료", matchId, userId);
//                } else {
//                    log.warn("매치[{}] 유저[{}] AI 분석 요청 실패 - 제출 데이터 없음", matchId, userId);
//                }
//            } catch (Exception e) {
//                // AI 분석 실패가 게임 흐름을 막지 않도록 예외 처리
//                log.error("AI 분석 요청 중 오류 발생: {}", e.getMessage(), e);
//            }
//
//            // 5. 아직 진행 중 (예: 5개 중 3번째 성공) -> "ING"
//            // success(true) : 성공
//            // success(false) : 실패 or 채점 중
//            return Judge0ResultDto.MatchResult.builder()
//                    .success(true)
//                    .message("Accepted")
//                    .stdout(failureStdout)
//                    .executedTime(maxTime)
//                    .executedMemory((int) maxMemory)
//                    .build();
//
//        } catch (Exception e) {
//            log.error("채점 결과 파싱 오류: {}", e.getMessage(), e);
//            throw new RuntimeException("채점 처리 실패", e);
//        }
//    }
}
