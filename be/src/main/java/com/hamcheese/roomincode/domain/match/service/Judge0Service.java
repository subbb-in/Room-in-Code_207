package com.hamcheese.roomincode.domain.match.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.problem.dao.ProblemDao;
import com.hamcheese.roomincode.domain.problem.dto.Problem;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class Judge0Service {
    private final ProblemDao problemDao;
    // redis에 테케 저장 필요
    private final MatchRedisRepository matchRedisRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${judge0.url}")
    private String judge0Url;

    @Value("${app.api.base-url}")
    private String serverBaseUrl;

    public Boolean gradeProblem(String matchId, String userId, Long problemId, String sourceCode,
            MatchLanguage language) throws Exception {
        // 1. DB에서 문제 정보(테스트케이스) 조회
        Problem problem = problemDao.findById(problemId);
        System.out.println(problem.getInputOutput());

        // 2. JSON 파싱 -> input_output -> inputs & outputs 분리
        log.info("problem.getInputOutput()={}", problem.getInputOutput());
        JsonNode ioNode = objectMapper.readTree(problem.getInputOutput());
        JsonNode inputs = ioNode.get("inputs");
        JsonNode outputs = ioNode.get("outputs");
        log.info("inputs node={}, outputs node={}", inputs, outputs);
        log.info("inputs size={}, outputs size={}",
                inputs == null ? "null" : inputs.size(),
                outputs == null ? "null" : outputs.size());

        if (inputs == null || outputs == null) {
            throw new IllegalStateException("테스트케이스 형식이 올바르지 않습니다.");
        }

        // + 총 테스트 케이스 개수
        int totalTestCases = inputs.size();
        matchRedisRepository.saveTestCaseRequirement(matchId, userId, totalTestCases);
        // 현재 통과한 개수는 0으로 세팅
        matchRedisRepository.initPassCount(matchId, userId);

        // 3. language id 구하기
        // {C++ (GCC 9.2.0) : 54, Python (3.8.1):71, Java (OpenJDK 13.0.1) : 62}
        int languageId = switch (language) {
            case CPP -> 54;
            case PYTHON -> 71;
            case JAVA -> 62;
        };

        // AI 분석용으로 소스코드/언어 Redis 저장
        matchRedisRepository.saveUserSubmission(matchId, userId, sourceCode, language.getValue());

        // 4. batch Submission으로 Judge0 post 호출
        // language_id 설정, wall_time_limit(Problem의 time_limit), memory_limit(Problem의
        // memory_limit) 설정
        // callback_url({matchId}) 설정, time(실행 시간), memory(실행 사용된 메모리), message(Internal
        // Error 발생 탐지)
        List<Map<String, Object>> submissions = new ArrayList<>();

        // 매치별 고유 콜백 URL 설정
        // 결과: http://localhost:8080/api/matches/callback?matchId=100&userId=userTest 형태
        String callbackUrl = UriComponentsBuilder.fromHttpUrl(serverBaseUrl)
                .path("/api/matches/callback")
                .queryParam("matchId", matchId)
                .queryParam("userId", userId)
                .build()
                .toUriString();

        List<String> inputList = new ArrayList<>(inputs.size());
        List<String> outputList = new ArrayList<>(outputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            inputList.add(normalizeFullWidthDigits(inputs.get(i).asText()));
        }
        for (int i = 0; i < outputs.size(); i++) {
            outputList.add(normalizeFullWidthDigits(outputs.get(i).asText()));
        }

        for (int i = 0; i < inputList.size(); i++) {
            Map<String, Object> sub = new HashMap<>();
            // Base64 인코딩 적용
            sub.put("source_code", Base64.getEncoder().encodeToString(sourceCode.getBytes()));
            sub.put("stdin", Base64.getEncoder().encodeToString(inputList.get(i).getBytes()));
            sub.put("expected_output", Base64.getEncoder().encodeToString(outputList.get(i).getBytes()));

            sub.put("language_id", languageId);

            // 문제 제약사항 (String -> Double 변환)
            double cpuLimit = parseTimeLimitSeconds(problem.getTimeLimit());
            log.info("Judge0 limits: timeLimitRaw='{}', cpuLimitParsed={}", problem.getTimeLimit(), cpuLimit);
            sub.put("cpu_time_limit", cpuLimit);
            // wall_time_limit은 CPU 시간보다 넉넉하게 설정 (시스템 오버헤드 고려)
            sub.put("wall_time_limit", cpuLimit * 2 + 5);
            int memoryLimitKb = parseMemoryLimitToKb(problem.getMemoryLimit());
            log.info("Judge0 limits: memoryLimitRaw='{}', memoryLimitKb={}", problem.getMemoryLimit(), memoryLimitKb);
            sub.put("memory_limit", memoryLimitKb);

            // 콜백 URL
            sub.put("callback_url", callbackUrl);

            submissions.add(sub);
        }
        log.info("submissions size={}", submissions.size());
        if (submissions.isEmpty()) {
            log.warn("Judge0 submissions is empty for matchId={}, userId={}", matchId, userId);
        } else {
            Map<String, Object> sample = submissions.get(0);
            log.info("Judge0 first submission keys: {}", sample.keySet());
            log.info("Judge0 first submission sizes: source_code={}, stdin={}, expected_output={}",
                    stringSize(sample.get("source_code")),
                    stringSize(sample.get("stdin")),
                    stringSize(sample.get("expected_output")));
        }
        // 5. Judge0 Batch Post 호출
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("submissions", submissions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Previous Map-based entity (kept for reference)
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        log.info("requestBody length={} bytes", requestBodyJson.getBytes().length);
        log.info("requestBody={}", requestBodyJson);

        try {
            // /submissions/batch 엔드포인트 호출, Base64 사용 명시
            String urlWithParams = UriComponentsBuilder.fromHttpUrl(judge0Url + "/submissions/batch")
                    .queryParam("base64_encoded", "true")
                    .toUriString();

            HttpEntity<String> jsonEntity = new HttpEntity<>(requestBodyJson, headers);
            restTemplate.postForEntity(urlWithParams, jsonEntity, String.class);
            log.info("채점 요청 성공: matchId={}, userId={}, caseCount={}", matchId, userId, totalTestCases);
            return true;
        } catch (Exception e) {
            log.error("Judge0 전송 실패", e);
            return false;
        }
    }

    private String normalizeFullWidthDigits(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value
                .replace('０', '0')
                .replace('１', '1')
                .replace('２', '2')
                .replace('３', '3')
                .replace('４', '4')
                .replace('５', '5')
                .replace('６', '6')
                .replace('７', '7')
                .replace('８', '8')
                .replace('９', '9');
    }

    private double parseTimeLimitSeconds(String rawLimit) {
        if (rawLimit == null || rawLimit.isBlank()) {
            log.warn("time_limit is empty; defaulting to 1.0s");
            return 1.0;
        }

        String numeric = rawLimit.replaceAll("[^0-9.]", "");
        if (numeric.isEmpty()) {
            log.warn("time_limit has no numeric part; defaulting to 1.0s. raw='{}'", rawLimit);
            return 1.0;
        }

        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException ex) {
            log.warn("time_limit parse failed; defaulting to 1.0s. raw='{}'", rawLimit);
            return 1.0;
        }
    }

    private int parseMemoryLimitToKb(String rawLimit) {
        if (rawLimit == null || rawLimit.isBlank()) {
            log.warn("memory_limit is empty; defaulting to 262144 KB");
            return 262144;
        }

        String normalized = rawLimit.trim().toLowerCase(Locale.ROOT);
        String numeric = normalized.replaceAll("[^0-9.]", "");
        if (numeric.isEmpty()) {
            log.warn("memory_limit has no numeric part; defaulting to 262144 KB. raw='{}'", rawLimit);
            return 262144;
        }
        double value;
        try {
            value = Double.parseDouble(numeric);
        } catch (NumberFormatException ex) {
            log.warn("memory_limit parse failed; defaulting to 262144 KB. raw='{}'", rawLimit);
            return 262144;
        }

        double kb;
        if (normalized.contains("gb") || normalized.contains("gigabyte") || normalized.contains("gigabytes")) {
            kb = value * 1024.0 * 1024.0;
        } else if (normalized.contains("mb") || normalized.contains("megabyte") || normalized.contains("megabytes")) {
            kb = value * 1024.0;
        } else if (normalized.contains("kb") || normalized.contains("kilobyte") || normalized.contains("kilobytes")) {
            kb = value;
        } else if (normalized.contains("byte") || normalized.contains("bytes")) {
            kb = value / 1024.0;
        } else {
            // Assume MB when unit is omitted.
            kb = value * 1024.0;
        }

        // Judge0 requires minimum 2048 KB
        int roundedKb = (int) Math.max(2048, Math.round(kb));
        int clampedKb = Math.min(512000, roundedKb);
        if (clampedKb != roundedKb) {
            log.warn("memory_limit clamped from {} KB to {} KB", roundedKb, clampedKb);
        }
        return clampedKb;
    }

    private int stringSize(Object value) {
        if (value == null) {
            return 0;
        }
        return value.toString().length();
    }
}
