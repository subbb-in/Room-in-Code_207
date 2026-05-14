package com.hamcheese.roomincode.domain.match.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.dto.AiRequestDto;
import com.hamcheese.roomincode.domain.match.dto.Judge0CallbackDto;
import com.hamcheese.roomincode.domain.match.dto.Judge0ResultDto;
import com.hamcheese.roomincode.domain.match.dto.SubmitRequestDto;
import com.hamcheese.roomincode.domain.match.handler.MatchWebSocketHandler;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.match.service.AiClientService;
import com.hamcheese.roomincode.domain.match.service.Judge0Service;
import com.hamcheese.roomincode.domain.match.service.MatchService;
import com.hamcheese.roomincode.global.dto.ApiResponse;
import com.hamcheese.roomincode.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@Slf4j
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchRedisRepository matchRedisRepository;
    private final Judge0Service judge0Service;
    private final ObjectMapper objectMapper;
    private final AiClientService aiClientService;

    @Lazy
    private final MatchWebSocketHandler matchWebSocketHandler;


    /**
     * 특정 매치의 OpenVidu 토큰 조회
     * 클라이언트가 매치 화면 진입 시 호출하여 화상 연결에 필요한 토큰을 받아감
     */
    @GetMapping("/{matchId}/openvidu/token")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOpenViduToken(
            @PathVariable String matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId().toString();
        String token = matchRedisRepository.getOpenViduToken(matchId, userId);
        String sessionId = matchRedisRepository.getOpenViduSessionId(matchId);

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TOKEN_NOT_FOUND", "OpenVidu 토큰을 찾을 수 없습니다.", 400));
        }

        Map<String, String> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("token", token);

        return ResponseEntity.ok(ApiResponse.success("OpenVidu 토큰 조회 성공", result));
    }

    /**
     * 코드 제출 - Judge0에 채점 요청
     */
    @PostMapping("/{matchId}/submit")
    public ResponseEntity<ApiResponse<Boolean>> submitCode(
            @PathVariable String matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SubmitRequestDto request
    ) {
        String userId = userDetails.getUserId().toString();
        log.info("코드 제출 요청 - matchId: {}, userId: {}, language: {}", matchId, userId, request.getLanguage());

        try {
            // 매치에 연결된 문제 ID 조회
            String problemIdStr = matchRedisRepository.getMatchMetaValue(matchId, "problem_id");
            if (problemIdStr == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("PROBLEM_NOT_FOUND", "매치에 연결된 문제가 없습니다.", 400));
            }
            Long problemId = Long.parseLong(problemIdStr);

            // Judge0에 채점 요청
            Boolean success = judge0Service.gradeProblem(
                    matchId, userId, problemId,
                    request.getSourceCode(), request.getLanguage()
            );

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("채점 요청 성공", true));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("JUDGE_REQUEST_FAILED", "채점 요청 실패", 500));
            }
        } catch (Exception e) {
            log.error("코드 제출 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("SUBMIT_ERROR", e.getMessage(), 500));
        }
    }

    /**
     * Judge0가 개별 테스트 케이스마다 채점 결과를 보내줌 (Callback)
     */
    @PutMapping("/callback")
    public ResponseEntity<String> receiveCallback(
            @RequestParam String matchId,
            @RequestParam String userId,
            @RequestBody Judge0CallbackDto callbackDto // String 대신 DTO로 바로 받기
    ) throws JsonProcessingException {
        log.info("Webhook 수신 - matchId: {}, userId: {}, status: {}",
                matchId, userId, callbackDto.getStatus().getDescription());

        // 1. 서비스에서 콜백 처리 (마지막 케이스일 때만 result가 채워짐)
        Judge0ResultDto.MatchResult result = matchService.handleCallback(matchId, userId, callbackDto);

        // 2. 아직 모든 테케가 끝나지 않았다면 응답만 하고 종료
        if (result == null) {
            return ResponseEntity.ok("Processing");
        }

        // 3. [최종 결과 발생] 웹소켓 메시지 생성 및 이벤트 발행
        Map<String, Object> socketMessage = new HashMap<>();
        socketMessage.put("type", "JUDGE_RESULT");
        socketMessage.put("userId", userId);
        socketMessage.put("success", result.isSuccess());
        socketMessage.put("message", result.getMessage());
        socketMessage.put("stdout", result.getStdout());
        socketMessage.put("execution", Map.of(
                "time", result.getExecutedTime(),
                "memory", result.getExecutedMemory()
        ));

        String payload = objectMapper.writeValueAsString(socketMessage);
        matchWebSocketHandler.broadcastToMatch(matchId, payload);

        // 4. 최종 결과에 따른 처리 (성공: 게임 종료 / 실패: AI 힌트)
        if (result.isSuccess()) {
            log.info("매치[{}] 최종 합격 - 게임 종료 처리", matchId);
            matchService.handleGradingSuccess(matchId);

            Map<String, Object> gameEndMessage = new HashMap<>();
            gameEndMessage.put("type", "GAME_END");
            gameEndMessage.put("matchId", matchId);
            gameEndMessage.put("message", "게임 종료! 결과를 확인하세요.");

            matchWebSocketHandler.broadcastToMatch(matchId, objectMapper.writeValueAsString(gameEndMessage));
        } else {
            log.info("매치[{}] 최종 오답 - AI 힌트 요청", matchId);

            // 오답 시에만 AI 힌트 분석 요청
            // Redis에서 유저 제출 정보 가져오기
            Map<Object, Object> userSubmission = matchRedisRepository.getUserSubmission(matchId, userId);
            Map<Object, Object> matchMeta = matchRedisRepository.getMatchMeta(matchId);
            Long problemId = matchMeta != null && matchMeta.get("problem_id") != null
                    ? Long.parseLong(matchMeta.get("problem_id").toString()) : null;

            AiRequestDto.AiHintRequestDto aiRequestDto = AiRequestDto.AiHintRequestDto.builder()
                    .matchId(matchId)
                    .userId(userId)
                    .problemId(problemId)
                    .sourceCode(userSubmission != null ? String.valueOf(userSubmission.get("sourceCode")) : "")
                    .language(userSubmission != null ? String.valueOf(userSubmission.get("language")) : "")
                    .build();
            aiClientService.aiHintRequest(aiRequestDto);
        }

        return ResponseEntity.ok("Final Processed");
    }
}
