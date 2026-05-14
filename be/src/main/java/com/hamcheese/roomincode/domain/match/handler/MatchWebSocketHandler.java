package com.hamcheese.roomincode.domain.match.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.dto.EmojiDto;
import com.hamcheese.roomincode.domain.match.dto.HintRequestDto;
import com.hamcheese.roomincode.domain.match.dto.HintResponseDto;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.match.service.MatchService;
import com.hamcheese.roomincode.domain.problem.dto.Problem;
import com.hamcheese.roomincode.global.handler.BaseWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchWebSocketHandler extends BaseWebSocketHandler {

    private final ObjectMapper objectMapper;

    // matchId별로 세션 관리 (matchId -> sessionId -> session)
    private final Map<String, Map<String, WebSocketSession>> matchSessions = new ConcurrentHashMap<>();
    private final MatchService matchService;
    private final SimpleAsyncTaskScheduler taskScheduler;
    private final MatchRedisRepository matchRedisRepository;


    /**
     * 연결 성립 시
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        Map<String, String> params = getQueryParams(session);
        String matchId = params.get("matchId");
        String userId = String.valueOf(session.getAttributes().get("userId")); // HandshakeInterceptor에서 넣어줌

        if (matchId == null) {
            log.warn("Connection rejected: matchId missing in params. Session: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 세션 속성에 저장
        session.getAttributes().put("matchId", matchId);

        // 세션 관리 맵에 등록 (JOIN 메시지에서 실제 참가자로 처리)
        matchSessions.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        log.info("Match WebSocket Connected - matchId: {}, userId: {}", matchId, userId);
    }

    /**
     * 메시지 수신 처리
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            String matchId = (String) session.getAttributes().get("matchId");
            String userId = String.valueOf(session.getAttributes().get("userId"));

            if (matchId == null) {
                log.warn("HandleText: matchId not found in session. User: {}", userId);
                return;
            }

            // 1. 우선 JsonNode로 읽어서 type 확인
            JsonNode jsonNode = objectMapper.readTree(payload);
            JsonNode typeNode = jsonNode.get("type");

            if (typeNode == null) {
                log.warn("Invalid payload: missing type");
                return;
            }

            String type = typeNode.asText();

            // final String finalMatchId = matchId; // No longer needed if we don't reassign
            // matchId

            // 3. 타입에 따른 분기 처리
            switch (type) {
                case "JOIN":
                    // 이미 등록된 세션인지 확인 (중복 JOIN 방지)
                    Map<String, WebSocketSession> sessions = matchSessions.get(matchId);
                    if (sessions == null || !sessions.containsKey(session.getId())) {
                        log.warn("매치[{}] 세션[{}]이 등록되지 않음", matchId, session.getId());
                        break;
                    }

                    // JOIN 처리 여부를 세션 속성으로 체크
                    if (session.getAttributes().containsKey("joined")) {
                        log.debug("매치[{}] 세션[{}] 중복 JOIN 무시", matchId, session.getId());
                        break;
                    }

                    session.getAttributes().put("joined", true);

                    // 1. 서비스에 시작 가능 여부 확인 (모든 인원이 모였는지 체크)
                    int currentCount = getJoinedCount(matchId);
                    Map<Object, Object> meta = matchService.attemptStartMatch(matchId, currentCount);

                    // 모든 참가자가 입장한 경우에만 참가자 목록 브로드캐스트 및 다음 단계 진행
                    if (meta != null) {
                        // 참가자 목록 브로드캐스트
                        java.util.List<String> currentUserIds = getMatchUserIds(matchId);
                        Map<String, Object> participantsPayload = new HashMap<>();
                        participantsPayload.put("type", "PARTICIPANTS_UPDATE");
                        participantsPayload.put("matchId", matchId);
                        participantsPayload.put("userIds", currentUserIds);
                        broadcastToMatch(matchId, objectMapper.writeValueAsString(participantsPayload));

                        // 상태에 따른 후속 처리
                        String stat = (String) meta.get("stat");

                        if ("VOTING".equals(stat)) {
                            // Broadcast Voting Start with Candidates
                            java.util.List<String> candidates = matchService.getAlgorithmCandidates(matchId);
                            String candidatesJson = objectMapper.writeValueAsString(candidates);
                            broadcastToMatch(matchId,
                                    "{\"type\":\"VOTING_START\", \"candidates\":" + candidatesJson + "}");
                        } else if ("DESIGN".equals(stat)) {
                            // + AI 모드인지 확인 여부를 위해 추가
                            String mode = (String) meta.get("mode");

                            // 2. 시작 가능하면 브로드캐스트
                            String difficulty = (String) meta.get("diff");
                            int designTime = calculateDesignTime(difficulty);
                            broadcastToMatch(matchId, "{\"type\":\"PHASE_CHANGED\", \"phase\":\"DESIGN\", \"duration\":"
                                    + designTime + "}");

                            // AI 모드가 아닐때만 타이머 시작
                            // -> AI 모드는 AI_READY 전까지 DESIGN
                            if (!"AI".equals(mode)) {
                                // 3. 타이머 예약 (다음 페이즈 이동)
                                taskScheduler.schedule(() -> triggerPlayingPhase(matchId),
                                        new java.util.Date(System.currentTimeMillis() + (designTime * 1000L)));
                            } else {
                                log.info("매치[{}] AI 모드 - 사용자 준비 대기 (자동 시작 안함)", matchId);
                            }

                        } else if ("PLAYING".equals(stat)) {
                            // [추가] PLAYING 중에 들어온 경우 처리
                            log.info("매치[{}] PLAYING 단계 중 세션 진입. 현재 상태 동기화.", matchId);

                            // 서비스에서 남은 시간(remainingTime)을 계산해서 meta에 넣어준다고 가정하거나,
                            // 여기서 직접 계산할 수 있습니다.
                            // 예: long remainingTime = matchService.getRemainingTime(matchId);

                            Integer remainingTime = (Integer) meta.get("remainingTime");
                            if (remainingTime == null)
                                remainingTime = 0; // 방어 코드

                            // 현재 게임 중임을 알리는 브로드캐스트
                            Map<String, Object> playingPayload = new HashMap<>();
                            playingPayload.put("type", "PHASE_CHANGED");
                            playingPayload.put("phase", "PLAYING");
                            playingPayload.put("duration", remainingTime);

                            // (필요하다면) 현재 풀어야 할 문제 정보 등 추가 데이터를 meta에서 꺼내 보낼 수 있음
                            // playingPayload.put("problem", meta.get("problem"));

                            broadcastToMatch(matchId, objectMapper.writeValueAsString(playingPayload));
                        }
                    }
                    break;

                // 프론트에서 유저가 준비완료 버튼 눌렀을 때
                case "AI_READY":
                    log.info("매치[{}] 유저[{}] AI 게임 시작 요청(준비 완료)", matchId, userId);
                    triggerPlayingPhase(matchId);
                    break;

                case "VOTE":
                    String votedAlgo = jsonNode.get("algo").asText();
                    matchService.processVote(matchId, userId, votedAlgo);

                    int connCount = getJoinedCount(matchId);
                    Map<Object, Object> pollMeta = matchService.attemptStartMatch(matchId, connCount);
                    if (pollMeta != null && "DESIGN".equals(pollMeta.get("stat"))) {
                        String difficulty = (String) pollMeta.get("diff");
                        int designTime = calculateDesignTime(difficulty);
                        broadcastToMatch(matchId,
                                "{\"type\":\"PHASE_CHANGED\", \"phase\":\"DESIGN\", \"duration\":" + designTime + "}");

                        // Schedule
                        taskScheduler.schedule(() -> triggerPlayingPhase(matchId),
                                new java.util.Date(System.currentTimeMillis() + (designTime * 1000L)));
                    }

                    break;

                case "TURN_END":
                    // 유저가 마지막으로 작성한 코드를 받아와서 저장 (동기화)
                    String userCode = jsonNode.get("code").asText();
                    matchRedisRepository.saveSharedCode(matchId, userCode);

                    // 턴 종료
                    // finishUserTurn -> rotateTurn -> 다음 AI 로직 실행
                    matchService.finishUserTurn(matchId, userId);
                    break;


                case "EMOJI":
                    EmojiDto emoji = objectMapper.treeToValue(jsonNode, EmojiDto.class);
                    log.info("매치[{}] 이모지: {}", matchId, emoji.getEmoji());
                    broadcastToMatch(matchId, payload);
                    break;

                case "CODE_UPDATE":
                    log.info("매치[{}] 코드 업데이트 발생", matchId);
                    broadcastToMatch(matchId, payload);
                    break;

                case "HINT_REQUEST":
                    try {
                        // 1. 요청 파싱 JSON을 HintRequestDto로 변환
                        HintRequestDto hintRequestDto = objectMapper.treeToValue(jsonNode, HintRequestDto.class);
                        HintRequestDto safeRequest = new HintRequestDto(
                                "HINT_REQUEST",
                                matchId,
                                userId,
                                hintRequestDto.getSequence());

                        // 2. 서비스 호출하여 HP 차감 및 힌트 조회
                        HintResponseDto hintResponseDto = matchService.useHint(safeRequest);

                        // 3. 힌트를 사용한 사람한테만 힌트 내용 응답
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(hintResponseDto)));

                        // 3. 해당 매치 방의 모든 유저에게 결과 브로드캐스트 (HP 동기화)
                        // 힌트 내용은 안 보이고 hp 변동된 것만 전체 참여자에게 보여주기
                        Map<String, Object> hpUpdate = new HashMap<>();
                        hpUpdate.put("type", "HP_UPDATE");
                        hpUpdate.put("userId", hintResponseDto.getUserId());
                        hpUpdate.put("remainingHp", hintResponseDto.getRemainingHp());

                        broadcastToMatch(hintResponseDto.getMatchId(), objectMapper.writeValueAsString(hpUpdate));
                    } catch (Exception ex) {
                        Map<String, Object> errorPayload = new HashMap<>();
                        errorPayload.put("type", "HINT_ERROR");
                        errorPayload.put("message", ex.getMessage());
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorPayload)));
                    }
                    break;

                case "PROBLEM_REQUEST":
                    try {
                        Problem problem = matchService.getMatchProblem(matchId);
                        Map<String, Object> problemMap = objectMapper.convertValue(problem, Map.class);
                        problemMap.remove("solutions");

                        // 캐싱된 한국어 번역 조회
                        String translatedProblem = matchService.getTranslatedProblem(matchId, "ko");

                        Map<String, Object> problemPayload = new HashMap<>();
                        problemPayload.put("type", "PROBLEM_RESPONSE");
                        problemPayload.put("matchId", matchId);
                        problemPayload.put("problem", problemMap);
                        problemPayload.put("translatedProblem", translatedProblem);

                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(problemPayload)));
                    } catch (IllegalStateException e) {
                        log.debug("매치[{}] 문제 정보 없음 (아직 할당 전일 수 있음): {}", matchId, e.getMessage());
                        // 클라이언트가 너무 빨리 요청했을 수 있으므로 에러 응답 없이 무시하거나,
                        // 필요하다면 "{type: 'WAITing'}" 같은 응답을 줄 수도 있음.
                        // 현 로직에서는 무시하여 클라이언트 에러 로그 방지.
                    }
                    break;

                case "LEAVE":
                    log.info("매치[{}] 유저[{}] 명시적 퇴장 요청", matchId, userId);

                    // 퇴장 처리 완료 플래그 설정 (afterConnectionClosed에서 중복 처리 방지)
                    session.getAttributes().put("leaveHandled", true);

                    // 1. 서비스 로직 호출 (Redis 제거, XP 페널티, 턴 관리 등)
                    matchService.leaveMatch(matchId, userId);

                    // 2. 다른 참가자들에게 알림
                    Map<String, Object> leaveData = new HashMap<>();
                    leaveData.put("type", "USER_LEFT");
                    leaveData.put("matchId", matchId);
                    leaveData.put("userId", userId);
                    broadcastToMatchExceptSender(matchId, new TextMessage(objectMapper.writeValueAsString(leaveData)),
                            session.getId());

                    // 3. 본인 세션 종료
                    session.close(CloseStatus.NORMAL);
                    matchSessions.get(matchId).remove(session.getId());
                    break;

                default:
                    log.warn("알 수 없는 메시지 타입: {}", type);
            }
        } catch (Exception e) {
            log.error("메시지 처리 에러", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String matchId = (String) session.getAttributes().get("matchId");
        String userId = String.valueOf(session.getAttributes().get("userId"));

        if (matchId == null) {
            log.warn("세션 {}에 matchId가 없음, 바이너리 메시지 무시", session.getId());
        }

        // [Yjs 연동 핵심]
        // 1. 현재 매치가 PLAYING 인가?
        // 2. 현재 턴이 이 유저(userId)인가? (MatchService 혹은 Redis 조회)
        // 3. 통과되면 다른 사람들에게 전송
        // 서비스에 권한 확인 후 브로드캐스트

        // 프론트에서 바이너리로 보낸 메시지는 yjs 통신용 데이터로 알아서 텍스트랑 구분한다고 함
        // 마찬가지로 프론트에서도 백에서 바이너리로 보내면 바이너리 데이터만 따로 handle 함
        if (matchService.canUserEdit(matchId, userId)) {
            broadcastToMatchExceptSender(matchId, message, session.getId());
        } else {
            log.trace("권한 없는 유저 {}의 수정 시도 무시됨", userId);
        }
    }

    /**
     * 연결 종료 시 세션 정리 (비정상 종료 포함)
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String sessionId = session.getId();
        String matchId = (String) session.getAttributes().get("matchId");
        String userId = String.valueOf(session.getAttributes().get("userId"));
        Boolean leaveHandled = (Boolean) session.getAttributes().get("leaveHandled");

        // 명시적 LEAVE로 이미 처리된 경우 스킵
        if (leaveHandled == null || !leaveHandled) {
            // [수정 사항] 비정상 종료 시 퇴장 처리 -> 재접속 허용을 위해 처리하지 않음
            // 단순 연결 끊김(새로고침, 네트워크 불안정 등)으로 간주하고,
            // Redis에서 유저 정보를 제거하지 않음.
            log.info("매치[{}] 유저[{}] 연결 종료 (status: {}). 재접속 대기.", matchId, userId, status);
        }

        // 세션 정리 (메모리 누수 방지)
        // matchSessions 맵에서 해당 세션만 제거
        if (matchId != null) {
            Map<String, WebSocketSession> sessions = matchSessions.get(matchId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    matchSessions.remove(matchId);
                    log.info("매치[{}] 세션 모두 종료됨. (게임 상태는 유지)", matchId);
                }
            }
        }

        log.debug("매치 세션 정리 완료 - sessionId: {}", sessionId);
    }

    /**
     * 에러 발생 시 처리
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        // 공통 에러 처리 (로그 + 세션 종료)는 부모 클래스에서 처리
        super.handleTransportError(session, exception);
    }

    /**
     * 특정 매치의 모든 참가자에게 메시지 전송
     */
    public void broadcastToMatch(String matchId, String payload) {
        Map<String, WebSocketSession> sessions = matchSessions.get(matchId);

        if (sessions != null) {
            sessions.values().forEach(session -> {
                synchronized (session) {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(payload));
                        }
                    } catch (Exception e) {
                        log.error("메시지 전송 실패 - sessionId: {}", session.getId(), e);
                    }
                }
            });
        }
    }

    public void broadcastToMatch(String matchId, WebSocketMessage<?> message) {
        Map<String, WebSocketSession> sessions = matchSessions.get(matchId);
        if (sessions != null) {
            sessions.values().forEach(session -> {
                synchronized (session) {
                    try {
                        if (session.isOpen())
                            session.sendMessage(message);
                    } catch (Exception e) {
                        log.error("메시지 전송 실패 - sessionId: {}", session.getId(), e);
                    }
                }
            });
        }
    }

    /**
     * 타이머에 의해 실행될 PLAYING 전환 로직
     */
    private void triggerPlayingPhase(String matchId) {
        // 1. 서비스 호출하여 비즈니스 로직(Redis) 처리 후 결과 받기
        String firstTurnUserId = matchService.startPlayingPhase(matchId);

        // 2. 결과 브로드캐스트
        String payload = "{\"type\":\"PHASE_CHANGED\", \"phase\":\"PLAYING\", \"currentTurn\":\"" + firstTurnUserId
                + "\"}";
        broadcastToMatch(matchId, payload);
    }

    private void broadcastToMatchExceptSender(String matchId, WebSocketMessage<?> message, String senderSessionId) {
        Map<String, WebSocketSession> sessions = matchSessions.get(matchId);
        if (sessions != null) {
            sessions.values().stream()
                    .filter(s -> !s.getId().equals(senderSessionId))
                    .forEach(s -> {
                        synchronized (s) {
                            try {
                                if (s.isOpen())
                                    s.sendMessage(message);
                            } catch (Exception e) {
                            }
                        }
                    });
        }
    }

    // 현재 세선 접속자 수 반환
    public int getConnectedCount(String matchId) {
        return matchSessions.getOrDefault(matchId, Collections.emptyMap()).size();
    }

    // 현재 JOIN 메시지를 보낸 사용자 수 반환
    public int getJoinedCount(String matchId) {
        Map<String, WebSocketSession> sessions = matchSessions.getOrDefault(matchId, Collections.emptyMap());
        return (int) sessions.values().stream()
                .filter(session -> session.getAttributes().containsKey("joined"))
                .count();
    }

    // 현재 매치의 모든 사용자 ID 반환
    public java.util.List<String> getMatchUserIds(String matchId) {
        Map<String, WebSocketSession> sessions = matchSessions.get(matchId);
        if (sessions == null)
            return java.util.Collections.emptyList();
        return sessions.values().stream()
                .map(session -> String.valueOf(session.getAttributes().get("userId")))
                .filter(userId -> userId != null && !userId.equals("null"))
                .toList();
    }

    private int calculateDesignTime(String difficulty) {
        if (difficulty == null)
            return 60;

        return switch (difficulty.toUpperCase().replace(" ", "_")) {
            case "EASY" -> 30;
            case "MEDIUM" -> 60;
            case "MEDIUM_HARD" -> 90;
            case "HARD" -> 120;
            case "VERY_HARD" -> 150;
            default -> 60;
        };
    }
}