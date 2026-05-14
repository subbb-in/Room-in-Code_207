package com.hamcheese.roomincode.domain.match;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.hamcheese.roomincode.domain.match.dto.HintRequestDto;
import com.hamcheese.roomincode.domain.match.dto.HintResponseDto;
import com.hamcheese.roomincode.domain.match.repository.MatchRedisRepository;
import com.hamcheese.roomincode.domain.match.service.MatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Mockito 사용 설정
class MatchServiceTest {

    @Mock
    private MatchRedisRepository matchRedisRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    @DisplayName("힌트 사용 성공: HP가 10 깎이고 힌트 내용이 나와야 함")
    void useHint_Success() {
        // 1. 가짜 데이터 준비 (Given)
        String matchId = "test-match";
        String userId = "user123";
        HintRequestDto request = new HintRequestDto("HINT_REQUEST", matchId, userId, 1);

        // Redis 동작 모사
        when(matchRedisRepository.getMatchMetaValue(matchId, "problem_id")).thenReturn("500");
        when(matchRedisRepository.getUserHp(matchId, userId)).thenReturn(100);
        when(matchRedisRepository.getHint(500L, 1)).thenReturn("이것은 힌트입니다.");
        when(matchRedisRepository.decrementUserHp(matchId, userId, 10)).thenReturn(90);

        // 2. 실행 (When)
        HintResponseDto result = matchService.useHint(request);

        // 3. 검증 (Then)
        assertEquals("이것은 힌트입니다.", result.getContent());
        assertEquals(90, result.getRemainingHp());
        verify(matchRedisRepository, times(1)).decrementUserHp(matchId, userId, 10);
    }

    @Test
    @DisplayName("힌트 사용 실패: HP가 이미 70이면 3번 다 쓴 것이므로 에러 발생")
    void useHint_Fail_LimitExceeded() {
        // Given
        String matchId = "test-match";
        String userId = "user123";
        HintRequestDto request = new HintRequestDto("HINT_REQUEST", matchId, userId, 4);

        when(matchRedisRepository.getUserHp(matchId, userId)).thenReturn(70);

        // When & Then (에러가 발생하는지 확인)
        assertThrows(IllegalStateException.class, () -> {
            matchService.useHint(request);
        });
    }
}