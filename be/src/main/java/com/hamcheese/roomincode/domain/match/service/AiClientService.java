package com.hamcheese.roomincode.domain.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.dao.MatchDao;
import com.hamcheese.roomincode.domain.match.dto.AiRequestDto;
import com.hamcheese.roomincode.domain.match.dto.AiResponseDto;
import com.hamcheese.roomincode.domain.match.event.AiReportEvent;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiClientService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final MatchRedisRepository matchRedisRepository;
    private final MatchDao matchDao;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    /**
     * [AI 턴 진행] AI에게 현재 코드 보내고, 다음 코드 이어받는다.
     * MatchService의 processAiTurn 호출
     */
    public String requestAiRelay(String problem, String currentCode, String language) {

        // 1. 요청 url
        String aiRelayUrl = aiServerUrl + "/ai-relay";
        log.info("AI Relay 요청 시작 - URL: {}, Lang: {}", aiRelayUrl, language);

        // 2. 요청 생성
        AiRequestDto.AiRelayRequestDto aiRelayRequestDto = AiRequestDto.AiRelayRequestDto.builder()
                .problem(problem)
                .code(currentCode)
                .language(language)
                .build();

        try {
            // 3. AI 서버 호출 (동기 방식 - 결과를 받아야 다음 로직이 진행되므로)
            // 릴레이는 게임 턴의 일부라 결과를 바로 리턴해야 해서 @Async를 여기다 붙이지 않고,
            // MatchService에서 이 메서드를 호출하는 감싸는 메서드에 @Async를 거는 게 낫습니다.
            AiResponseDto.AiRelayResponseDto response = restTemplate.postForObject(aiRelayUrl, aiRelayRequestDto, AiResponseDto.AiRelayResponseDto.class);

            if (response != null && response.getGeneratedCode() != null) {
                log.info("AI Relay 응답 성공 - 코드 길이: {}", response.getGeneratedCode().length());
                return response.getGeneratedCode();
            } else {
                log.warn("AI Relay 응답이 비어있음. 기존 코드 반환");
                return currentCode;
            }

        } catch (Exception e) {
            log.error("AI Relay 서버 통신 실패: {}", e.getMessage());
            // 실패 시 게임이 멈추지 않도록 기존 코드를 그대로 반환
            return currentCode;
        }

    }

    /**
     * AI 서버로 매치 분석 요청을 보냅니다.
     * @param request 분석 요청 데이터
     */
    @Async
    public void analyzeMatch(AiRequestDto.AiFeedbackRequestDto request) {
        log.info("AI 서버 분석 요청 시작 - MatchId: {}, UserId: {}", request.getMatchId(), request.getUserId());
        log.info("분석 데이터: ProblemId={}, TimeComplexity={}, MemUsage={}, Lang={}",
                request.getProblemId(), request.getExecutedTime(), request.getExecutedMemory(), request.getLanguage());

        try {
            // POST 요청 전송
            AiResponseDto.AiFeedbackDto response = restTemplate.postForObject(aiServerUrl+"/ai-feedback", request, AiResponseDto.AiFeedbackDto.class);

            if (response != null) {
                log.info("AI 분석 완료 - Feedback Provided");

                // AI Feedback (DTO → Map으로 직렬화, 프론트와 키 일치)
                Map<String, Object> aiFeedbackMap = new HashMap<>();
                aiFeedbackMap.put("comment", response.getComment());
                aiFeedbackMap.put("refactored_code", response.getRefactoredCode());
                aiFeedbackMap.put("recommendation", response.getRecommendation());
                String aiFeedbackJson = objectMapper.writeValueAsString(aiFeedbackMap);

                // Graded Result
                Map<String, Object> gradedMap = new HashMap<>();
                gradedMap.put("time", request.getExecutedTime());
                gradedMap.put("memory", request.getExecutedMemory());

                String gradedResultJson = objectMapper.writeValueAsString(gradedMap);

                // Game Result
                Map<String, Object> gameMap = new HashMap<>();
                gameMap.put("matchId", request.getMatchId());
                gameMap.put("userId", request.getUserId());

                
                String gameResultJson = objectMapper.writeValueAsString(gameMap);

                // 모든 참여자 조회
                Set<String> matchUsers = matchRedisRepository.getMatchUsers(request.getMatchId());

                // 각 참여자별로 DB 저장
                for (String participantId : matchUsers) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("userId", Long.parseLong(participantId));
                    params.put("problemId", request.getProblemId());
                    //params.put("skillId", request.getSkillId());

                    //matchDao.saveHistory(params);

                    //Long historyId = (Long) params.get("historyId");

                    //params.put("historyId", historyId);
                    params.put("matchId", request.getMatchId());
                    params.put("problemTitle", "");
                    params.put("aiFeedback", aiFeedbackJson);
                    params.put("gameResult", gameResultJson);
                    params.put("gradedResult", gradedResultJson);

                    matchDao.saveAiReport(params);
                }
                log.info("AI 리포트 저장 완료 - {}명", matchUsers.size());

                // WebSocket 브로드캐스팅
                Map<String, Object> socketMessage = new HashMap<>();
                socketMessage.put("type", "AI_REPORT");
                socketMessage.put("matchId", request.getMatchId());
                socketMessage.put("userId", request.getUserId());
                socketMessage.put("data", Map.of(
                        "aiFeedback", aiFeedbackMap,
                        "gameResult", gameMap,
                        "gradedResult", gradedMap
                ));

                String payload = objectMapper.writeValueAsString(socketMessage);
                eventPublisher.publishEvent(
                        new AiReportEvent(request.getMatchId(), payload)
                );                log.info("AI 리포트 웹소켓 전송 완료");
            } else {
                log.warn("AI 서버로부터 응답이 없습니다.");
            }

        } catch (Exception e) {
            // 게임 로직 방해하지 않도록 예외 안 던짐
            log.error("AI 서버 통신 및 저장 중 오류 발생: {}", e.getMessage(), e);
        }
        log.info("AI 서버 분석 요청 처리 종료");
    }

    /**
     * AI 서버로 AI 힌트 요청을 보냅니다.
     * @param request 분석 요청 코드
     */
    public void aiHintRequest(AiRequestDto.AiHintRequestDto request) {
        log.info("AI 서버 분석 요청 시작 - MatchId: {}, UserId: {}", request.getMatchId(), request.getUserId());
        log.info("[DEBUG] 현재 aiServerUrl 값: '{}'", aiServerUrl);

        try {

            // 테스트 목적: request에 sourceCode와 language가 이미 있으면 Redis 조회 생략
            String sourceCode = request.getSourceCode();
            String language = request.getLanguage();

            if (sourceCode == null || sourceCode.isEmpty() || language == null || language.isEmpty()) {
                // 제출 코드와 언어를 redis에서 꺼내고 controller에서 받아오지 못한 request에 set
                Map<Object, Object> submission = matchRedisRepository.getUserSubmission(request.getMatchId(), request.getUserId());
                sourceCode = String.valueOf(submission.get("sourceCode"));
                language = String.valueOf(submission.get("language"));
                request.setSourceCode(sourceCode);
                request.setLanguage(language);
            }


            // POST 요청 전송
            AiResponseDto.AiHintDto response = restTemplate.postForObject(aiServerUrl + "/ai-hint", request, AiResponseDto.AiHintDto.class);

            // 결과를 받으면
            if (response != null) {
                log.info("AI 분석 완료 - Feedback Provided");

                // ai hint
                //String aiHint = objectMapper.writeValueAsString(response.getAiHint());

                // WebSocket 브로드캐스팅
                Map<String, Object> socketMessage = new HashMap<>();
                socketMessage.put("type", "AI_HINT");
                socketMessage.put("matchId", request.getMatchId());
                socketMessage.put("userId", request.getUserId());
                socketMessage.put("data", response);

                String payload = objectMapper.writeValueAsString(socketMessage);
                eventPublisher.publishEvent(
                        new AiReportEvent(request.getMatchId(), payload)
                );
            }
        } catch (Exception e) {
            log.error("AI 서버 통신 및 저장 중 오류 발생: {}", e.getMessage(),e);
        }
        log.info("AI 힌트 요청 처리 종료");
    }
}
