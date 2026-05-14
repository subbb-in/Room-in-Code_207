package com.hamcheese.roomincode.global.infra.openvidu;

import com.hamcheese.roomincode.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class OpenViduController {

    private final OpenViduService openViduService;

    // 세션 생성
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createSession() {
        try {
            String sessionId = openViduService.createSession();
            return ResponseEntity.ok(ApiResponse.success("세션이 성공적으로 생성되었습니다.", sessionId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "OPENVIDU_CREATE_ERROR",
                            "세션 생성 중 오류가 발생했습니다."+e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }

    // 토큰 생성
    @PostMapping("/{sessionId}/token")
    public ResponseEntity<ApiResponse<String>> generateToken(@PathVariable String sessionId){
        try {
            String token = openViduService.generateToken(sessionId);
            return ResponseEntity.ok(ApiResponse.success("토큰이 성공적으로 발행되었습니다.", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "OPENVIDU_TOKEN_ERROR",
                            "토큰 발행 실패: " + e.getMessage(),
                            HttpStatus.BAD_REQUEST.value()
                    ));
        }
    }
}
