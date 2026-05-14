package com.hamcheese.roomincode.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamcheese.roomincode.domain.user.dto.PasswordChangeDto;
import com.hamcheese.roomincode.domain.user.dto.UserProfileDto;
import com.hamcheese.roomincode.domain.user.dto.UserRequestDto;
import com.hamcheese.roomincode.domain.user.service.UserService;
import com.hamcheese.roomincode.global.dto.ApiResponse;
import com.hamcheese.roomincode.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Log4j2
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // 이메일 인증 - 1. 인증번호 요청
    @PostMapping("/email-verification/request")
    public ResponseEntity<ApiResponse<Void>> requestVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증번호가 발송되었습니다."));
    }

    // 이메일 인증 - 2. 인증번호 확인
    @PostMapping("/email-verification/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        boolean verified = userService.verifyCode(email, code);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("인증되었습니다."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("VERIFICATION_FAILED", "인증번호가 틀리거나 만료되었습니다.", 400));
        }
    }

    // 회원가입
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody UserRequestDto.SignUp requestDto) {
        userService.signUp(requestDto);
        return ResponseEntity.ok(ApiResponse.success("회원가입 완료"));
    }

    // 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserProfileDto profile = userService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 프로필 조회 성공", profile));
    }


    // 특정 유저 프로필 조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(@PathVariable Long userId) {
        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", profile));
    }

    //프로필 수정(내꺼)
    @PatchMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(value = "profile") String profileString,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        // 현재 로그인된 유저 ID 추출
        Long userId = userDetails.getUserId();

        try {
            // ObjectMapper를 사용하여 JSON 문자열을 DTO로 변환
            UserProfileDto.UpdateRequest updateDto = objectMapper.readValue(profileString, UserProfileDto.UpdateRequest.class);

            // updateProfile 서비스 호출
            userService.updateProfile(userId, updateDto, file);

            // 수정된 최신 프로필 정보 조회 및 반환
            UserProfileDto updatedProfile = userService.getProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("프로필 수정 완료", updatedProfile));

        } catch (IOException e) {
            // 파일 처리 관련 에러 응답 (요청하신 양식 적용)
            log.error("파일 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "FILE_UPLOAD_ERROR",
                            "프로필 이미지 저장 중 오류가 발생했습니다: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        } catch (Exception ex) {
            // 기타 일반 서버 오류 응답
            log.error("서버 내부 오류: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "SERVER_INTERNAL_ERROR",
                            "서버에서 오류가 발생했습니다: " + ex.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }

    }

    //패스워드 변경
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PasswordChangeDto dto) {
        userService.changePassword(userDetails.getUserId(), dto);
        return ResponseEntity.ok(ApiResponse.success("비밀번호 변경 완료"));
    }

    // 회원 탈퇴
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody UserRequestDto.Delete dto) {
        userService.softDeleteUser(dto);
        return ResponseEntity.ok(
                ApiResponse.success("회원 탈퇴 처리가 완료되었습니다."));
    }

    // 비밀번호 초기화 링크 요청
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid UserRequestDto.ResetPassword dto) {

        userService.sendResetLink(dto.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호 초기화 이메일이 전송되었습니다.")
        );
    }

    // 비밀번호 재설정 토큰 유효성 검증 : 페이지 진입용
    @GetMapping("/password/reset/check")
    public ResponseEntity<ApiResponse<Void>> checkToken(
            @RequestParam("token") String token) {
        userService.validateToken(token);

        return ResponseEntity.ok(ApiResponse.success("유효한 토큰입니다.")
        );
    }

    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam("token") String token,
            @RequestBody @Valid UserRequestDto.NewPassword dto
    ) {
        userService.resetPassword(token, dto.getNewPassword());
        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.")
        );
    }
}