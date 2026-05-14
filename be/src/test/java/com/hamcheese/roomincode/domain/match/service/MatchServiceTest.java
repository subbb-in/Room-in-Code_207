package com.hamcheese.roomincode.domain.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.match.dto.MatchInitDto;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.problem.dao.ProblemDao;
import com.hamcheese.roomincode.domain.problem.dto.Problem;
import com.hamcheese.roomincode.domain.problem.dto.ProblemHint;
import com.hamcheese.roomincode.domain.match.service.MatchTimerService;
import com.hamcheese.roomincode.domain.match.service.AiClientService;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.service.UserService;
import com.hamcheese.roomincode.global.infra.papago.PapagoService;
import com.hamcheese.roomincode.global.infra.openvidu.OpenViduService;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

        @InjectMocks
        private MatchService matchService;

        @Mock
        private MatchRedisRepository matchRedisRepository;

        @Mock
        private ProblemDao problemDao;

        @Mock
        private ObjectMapper objectMapper;

        @Mock
        private OpenViduService openViduService;

        @Mock
        private MatchTimerService matchTimerService;

        @Mock
        private AiClientService aiClientService;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        @Mock
        private UserDao userDao;

        @Mock
        private UserService userService;

        @Mock
        private PapagoService papagoService;

        @Test
        @DisplayName("매치 초기화 성공 - 문제와 힌트가 Redis에 정상적으로 저장되어야 한다.")
        void initializeMatch_Success() throws JsonProcessingException {
                // given
                MatchInitDto initDto = MatchInitDto.builder()
                                .matchId("test-match-id")
                                .algo("DFS")
                                .diff("LV2")
                                .lang(MatchLanguage.JAVA)
                                .mode("FRIENDS")
                                .userIds(List.of("user1", "user2"))
                                .build();

                Problem problem = new Problem(
                                100L,
                                "Test Question",
                                "Test Solution",
                                "EASY",
                                "source",
                                "url",
                                "1s",
                                "256MB",
                                "{}",
                                "O(n)",
                                "O(n^2)");

                ProblemHint hint = new ProblemHint(1L, 1, "Test Hint Content", 100L);

                given(problemDao.findRandomProblem(anyString(), anyString())).willReturn(problem);
                given(problemDao.findHintsByProblemId(100L)).willReturn(List.of(hint));
                given(objectMapper.writeValueAsString(problem)).willReturn("{\"problemId\":100}");
                given(matchRedisRepository.getProblemCache(anyLong())).willReturn("{\"problemId\":100}");
                given(papagoService.translate(anyString(), anyString(), anyString()))
                                .willReturn(Mono.just("Translated Question"));

                // when
                matchService.initializeMatch(initDto);

                // then
                // 1. 문제 캐싱 확인
                verify(matchRedisRepository).saveProblemCache(eq(100L), anyString());
                // 2. 힌트 캐싱 확인
                verify(matchRedisRepository).saveHintCache(eq(100L), eq(1), eq("Test Hint Content"));
                // 3. 메타 정보 저장 확인 (Map 내부 값 검증은 생략 혹은 ArgumentCaptor 활용 가능)
                verify(matchRedisRepository).saveMatchMeta(eq("test-match-id"), anyMap());
                // 4. 참여자 저장 확인
                verify(matchRedisRepository).saveMatchUsers(eq("test-match-id"), eq(initDto.getUserIds()));
                // 5. 참여자 인원수 맞는지 확인
                ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
                verify(matchRedisRepository).saveMatchUsers(eq("test-match-id"), captor.capture());

                List<String> capturedUserIds = captor.getValue();
                assertEquals(initDto.getUserIds().size(), capturedUserIds.size());

        }

        @Test
        @DisplayName("매치 초기화 실패 - 조건에 맞는 문제가 없을 경우 IllegalStateException 발생")
        void initializeMatch_Fail_NoProblem() {
                // given
                MatchInitDto initDto = MatchInitDto.builder()
                                .algo("DFS")
                                .diff("LV99")
                                .lang(MatchLanguage.JAVA)
                                .mode("FRIENDS")
                                .build();

                given(problemDao.findRandomProblem(anyString(), anyString())).willReturn(null);

                // when & then
                assertThrows(IllegalStateException.class, () -> matchService.initializeMatch(initDto));

                // Redis 저장 로직이 호출되지 않았는지 확인
                verify(matchRedisRepository, never()).saveMatchMeta(anyString(), anyMap());
        }

        @Test
        @DisplayName("문제 할당 실패 - JSON 변환 오류 시 RuntimeException 발생")
        void assignProblemToMatch_Fail_JsonError() throws JsonProcessingException {
                // given
                Problem problem = new Problem(
                                100L, "Test Question", "Test Solution", "EASY",
                                "source", "url", "1s", "256MB", "{}", "O(n)", "O(n^2)");

                given(problemDao.findRandomProblem(anyString(), anyString())).willReturn(problem);
                given(objectMapper.writeValueAsString(any()))
                                .willThrow(new JsonMappingException("Error"));

                // when & then
                assertThrows(RuntimeException.class,
                                () -> matchService.assignProblemAfterVote("match-id", "DFS", "LV2"));
        }
}