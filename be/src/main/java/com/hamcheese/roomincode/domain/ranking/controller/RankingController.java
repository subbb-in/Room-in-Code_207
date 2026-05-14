package com.hamcheese.roomincode.domain.ranking.controller;

import com.hamcheese.roomincode.domain.ranking.dto.RankingInfoResponseDto;
import com.hamcheese.roomincode.domain.ranking.service.RankingService;
import com.hamcheese.roomincode.global.dto.ApiResponse;
import com.hamcheese.roomincode.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<RankingInfoResponseDto>>> getRankingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("랭킹 조회 성공", rankingService.getRankingList(page, size)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<RankingInfoResponseDto>> getUserRank(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userId + "번 유저 랭킹 조회 성공", rankingService.getUserRank(userId)));
    }
}
