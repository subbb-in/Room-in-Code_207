package com.hamcheese.roomincode.domain.ranking.service;

import com.hamcheese.roomincode.domain.ranking.dto.RankingInfoResponseDto;
import com.hamcheese.roomincode.domain.ranking.repository.RankingRedisRepository;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.dto.UserProfileDto;
import com.hamcheese.roomincode.global.dto.PageResponseDto;
import com.hamcheese.roomincode.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RankingRedisRepository rankingRedisRepository;
    private final UserDao userDao;

    public PageResponseDto<RankingInfoResponseDto> getRankingList(int page, int size) {
        //1. 전체 랭커 수 조회
        Long totalElements = rankingRedisRepository.getTotalRankerCount();
        if (totalElements == null) {
            totalElements = 0L;
        }
        int totalPages = (int) Math.ceil((double) totalElements / size);

        //2. 페이징을 위한 시작, 끝 인덱스 계산
        long start = (long) (page - 1) * size;
        long stop = start + size - 1;

        //3. Redis에서 해당 페이지 랭커 조회
        Set<ZSetOperations.TypedTuple<String>> rankers = rankingRedisRepository.getRankersByRange(start, stop);
        List<RankingInfoResponseDto> rankingList = new ArrayList<>();

        //4. 등수는 start 인덱스 기준
        AtomicLong currentRank = new AtomicLong(start + 1);

        for (ZSetOperations.TypedTuple<String> ranker : rankers) {
            String userId = ranker.getValue();
            UserProfileDto userProfileDto = userDao.findProfileByUserId(Long.parseLong(userId, 16));

            if (userProfileDto != null) {
                rankingList.add(RankingInfoResponseDto.builder()
                        .rank(currentRank.getAndIncrement())
                        .userId(userProfileDto.getUserId())
                        .nickname(userProfileDto.getNickname())
                        .xp(Objects.requireNonNull(ranker.getScore()).intValue())
                        .introText(userProfileDto.getIntroText())
                        .profileImageUrl(userProfileDto.getProfileImageUrl())
                        .build());
            }
        }

        //5. PageResponseDto로 감싸서 반환
        return PageResponseDto.<RankingInfoResponseDto>builder()
                .currentPage(page)
                .totalPage(totalPages)
                .totalElements(totalElements)
                .content(rankingList)
                .build();
    }

    public RankingInfoResponseDto getUserRank(Long userId) {
        Long rank = rankingRedisRepository.getUserRank(userId);
        if (rank == null) {
            throw new ResourceNotFoundException("랭킹에 존재하지 않는 유저");
        }

        UserProfileDto userProfileDto = userDao.findProfileByUserId(userId);
        if (userProfileDto == null) {
            throw new ResourceNotFoundException("존재하지 않는 유저");
        }

        return RankingInfoResponseDto.builder()
                .rank(rank + 1)
                .userId(userProfileDto.getUserId())
                .nickname(userProfileDto.getNickname())
                .xp(userProfileDto.getTotalXp())
                .introText(userProfileDto.getIntroText())
                .profileImageUrl(userProfileDto.getProfileImageUrl())
                .build();
    }
}
