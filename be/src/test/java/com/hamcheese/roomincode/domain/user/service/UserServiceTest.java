package com.hamcheese.roomincode.domain.user.service;

import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.dto.UserRequestDto;
import com.hamcheese.roomincode.domain.user.entity.UserEntity;
import com.hamcheese.roomincode.domain.user.repository.VerificationCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional //테스트 후 DB 롤백
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUp_Success() {
        // given: 회원가입 정보가 주어졌을 때
        UserRequestDto.SignUp requestDto = new UserRequestDto.SignUp();
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("password123");
        requestDto.setNickname("testnick");

        // 이메일 인증을 통과했다고 가정
        verificationCodeRepository.saveCode(requestDto.getEmail(), "dummy");
        verificationCodeRepository.markVerified(requestDto.getEmail());

        // when: 회원가입을 실행하면
        userService.signUp(requestDto);

        // then: 유저 정보가 DB에 올바르게 저장되어야 한다.
        UserEntity foundUser = userDao.findByEmail(requestDto.getEmail());

        assertNotNull(foundUser, "유저가 DB에서 조회되어야 합니다.");
        assertEquals(requestDto.getEmail(), foundUser.getEmail(), "저장된 이메일이 일치해야 합니다.");
        assertTrue(passwordEncoder.matches(requestDto.getPassword(), foundUser.getPassword()), "비밀번호가 암호화되어 저장되어야 합니다.");
        
        // 닉네임은 다른 테이블에 저장되므로, existsByNickname으로 존재 여부만 확인
        assertTrue(userDao.existsByNickname(requestDto.getNickname()), "저장된 닉네임이 존재해야 합니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signUp_Fail_DuplicateEmail() {
        // given: 먼저 사용자를 하나 생성
        UserRequestDto.SignUp originalUser = new UserRequestDto.SignUp();
        originalUser.setEmail("duplicate@example.com");
        originalUser.setPassword("password123");
        originalUser.setNickname("original_nick");
        verificationCodeRepository.markVerified(originalUser.getEmail());
        userService.signUp(originalUser);

        // when: 같은 이메일로 다시 가입을 시도할 때
        UserRequestDto.SignUp newUserWithSameEmail = new UserRequestDto.SignUp();
        newUserWithSameEmail.setEmail("duplicate@example.com");
        newUserWithSameEmail.setPassword("new_password");
        newUserWithSameEmail.setNickname("new_nick");
        verificationCodeRepository.markVerified(newUserWithSameEmail.getEmail());

        // then: IllegalArgumentException이 발생해야 한다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(newUserWithSameEmail);
        });
        assertEquals("이미 가입된 이메일입니다", exception.getMessage());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 닉네임")
    void signUp_Fail_DuplicateNickname() {
        // given: 먼저 사용자를 하나 생성
        UserRequestDto.SignUp originalUser = new UserRequestDto.SignUp();
        originalUser.setEmail("original@example.com");
        originalUser.setPassword("password123");
        originalUser.setNickname("duplicate_nick");
        verificationCodeRepository.markVerified(originalUser.getEmail());
        userService.signUp(originalUser);

        // when: 같은 닉네임으로 다시 가입을 시도할 때
        UserRequestDto.SignUp newUserWithSameNickname = new UserRequestDto.SignUp();
        newUserWithSameNickname.setEmail("new@example.com");
        newUserWithSameNickname.setPassword("new_password");
        newUserWithSameNickname.setNickname("duplicate_nick");
        verificationCodeRepository.markVerified(newUserWithSameNickname.getEmail());

        // then: IllegalArgumentException이 발생해야 한다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(newUserWithSameNickname);
        });
        assertEquals("이미 사용중인 닉네임입니다", exception.getMessage());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 미인증")
    void signUp_Fail_EmailNotVerified() {
        // given: 이메일 인증을 하지 않은 사용자 정보
        UserRequestDto.SignUp requestDto = new UserRequestDto.SignUp();
        requestDto.setEmail("unverified@example.com");
        requestDto.setPassword("password123");
        requestDto.setNickname("unverified_nick");

        // when & then: 회원가입을 시도하면 IllegalArgumentException이 발생해야 한다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(requestDto);
        });
        assertEquals("이메일 인증을 먼저 해주세요", exception.getMessage());
    }
}
