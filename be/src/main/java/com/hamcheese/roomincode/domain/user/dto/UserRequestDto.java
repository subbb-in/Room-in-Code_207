package com.hamcheese.roomincode.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class UserRequestDto {
    private Long userId;
    private String email;
    private String password;
    private String nickname;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SignUp {
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
        @NotBlank
        private String nickname;
    }

    // UserRequestDto.java
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProfileUpdate {
        private String nickname;
        private String profileImageUrl;
        private String introText;
    }

    // 회원 탈퇴
    @Getter
    @Setter
    public static class Delete {
        private Long userId;
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    // 비밀번호 초기화 메일 요청
    @Getter
    @NoArgsConstructor
    public static class ResetPassword {
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;
    }

    // 비밀번호 재설정
    @Getter
    @NoArgsConstructor
    public static class NewPassword {
        @NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
        private String newPassword;
    }
}
