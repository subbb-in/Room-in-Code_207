package com.hamcheese.roomincode.domain.room.controller;

import com.hamcheese.roomincode.domain.match.type.MatchLanguage;
import com.hamcheese.roomincode.domain.room.dto.RoomRequestDto;
import com.hamcheese.roomincode.domain.room.dto.RoomResponseDto;
import com.hamcheese.roomincode.domain.room.service.RoomService;
import com.hamcheese.roomincode.global.dto.ApiResponse;
import com.hamcheese.roomincode.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    // 방 입장
    @PostMapping("/{roomId}/participants")
    public ResponseEntity<ApiResponse<String>> joinRoom(
            @PathVariable String roomId,
            @RequestBody RoomRequestDto.Join requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();
        try {
            roomService.joinRoom(roomId, userId, requestDto);
            return ResponseEntity.ok(ApiResponse.success("방 입장 성공"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("BAD_REQUEST", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", 500));
        }
    }

    // 방 생성
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponseDto.RoomCreateResponse>> createRoom (
            @jakarta.validation.Valid @RequestBody RoomRequestDto.RoomCreateRequest requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails

    ){
        RoomResponseDto.RoomCreateResponse response = roomService.createRoom(requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("방이 생성되었습니다.", response));
    }

    // 방 나가기
    @DeleteMapping("/{roomId}/participants/{userId}")
    public ResponseEntity<RoomResponseDto.RoomLeaveResponse> leaveRoom(
            @PathVariable String roomId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        RoomResponseDto.RoomLeaveResponse response = roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(response);
    }

    // 방 목록 조회하기
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponseDto.Info>>> getRoomList(
            @ModelAttribute RoomRequestDto.List dto) {
        try {
            List<RoomResponseDto.Info> rooms = roomService.getRoomList(dto);
            return ResponseEntity.ok(ApiResponse.success("방 목록을 조회했습니다.", rooms));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("INTERNAL_SERVER_ERROR", "방 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), 500));
        }
    }

    // 연습모드 방 입장 (AI 모드)
    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<RoomResponseDto.RoomCreateResponse>> createAiRoom(
            @Valid @RequestBody RoomRequestDto.AiRoomCreateRequest requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RoomResponseDto.RoomCreateResponse response = roomService.createAiRoom(requestDto, userDetails.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("AI 연습모드 방이 생성되어 입장했습니다.", response));
    }

    // ====== 랭킹전 대기열 (Contest Queue) ======
    private final com.hamcheese.roomincode.domain.match.service.ContestService contestService;

    @PostMapping("/contest/queue")
    public ResponseEntity<ApiResponse<String>> joinContestQueue(
            @RequestParam String language,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchLanguage matchLanguage = MatchLanguage.valueOf(language.toUpperCase());
        contestService.joinQueue(userDetails.getUserId(), matchLanguage);
        return ResponseEntity.ok(ApiResponse.success("대기열에 입장했습니다."));
    }

    @DeleteMapping("/contest/queue")
    public ResponseEntity<ApiResponse<String>> cancelContestQueue(
            @RequestParam String language,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchLanguage matchLanguage = MatchLanguage.valueOf(language.toUpperCase());
        contestService.cancelQueue(userDetails.getUserId(), matchLanguage);
        return ResponseEntity.ok(ApiResponse.success("대기열을 취소했습니다."));
    }
}