package com.hamcheese.roomincode.domain.user.service;

import com.hamcheese.roomincode.domain.ranking.repository.RankingRedisRepository;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.dto.PasswordChangeDto;
import com.hamcheese.roomincode.domain.user.dto.SkillStatsDto;
import com.hamcheese.roomincode.domain.user.dto.UserProfileDto;
import com.hamcheese.roomincode.domain.user.dto.UserRequestDto;
import com.hamcheese.roomincode.domain.user.entity.UserEntity;
import com.hamcheese.roomincode.domain.user.repository.PasswordResetRepository;
import com.hamcheese.roomincode.domain.user.repository.VerificationCodeRepository;
import com.hamcheese.roomincode.global.exception.ResourceNotFoundException;
import com.hamcheese.roomincode.global.util.EmailSender;
import com.hamcheese.roomincode.global.util.FileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final VerificationCodeRepository verificationCodeRepository; //인터페이스
    private final EmailSender emailSender;
    private final UserDao userDao;
    private final RankingRedisRepository rankingRedisRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRepository passwordResetRepository;
    private final FileStore fileStore;

    // 인증번호 발송
    @Transactional
    public void sendVerificationCode(String email) {
        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String code = String.valueOf((int) (Math.random() * 899999 + 100000));

        verificationCodeRepository.saveCode(email, code);

        emailSender.sendSignUpVerifyEmail(email, code);
    }

    // 인증번호 확인
    @Transactional
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = verificationCodeRepository.getCode(email);

        if (storedCode != null && storedCode.equals(inputCode)) {
            verificationCodeRepository.markVerified(email);
            return true;
        }
        return false;
    }

    // 회원가입
    @Transactional
    public void signUp(UserRequestDto.SignUp request) {
        //인증 여부 확인 (메모리로부터)
        if (!verificationCodeRepository.isVerified(request.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 먼저 해주세요");
        }

        //중복 체크(이메일, 닉네임)
        if (userDao.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }
        if (userDao.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다");
        }
        // users 테이블 저장
        UserEntity newUser = new UserEntity();
        newUser.setLoginType("LOGIN"); // 로그인 타입 LOGIN
        userDao.saveUser(newUser);
            //실행후 AutoIncrement id를 newUser.userId에 담아줌
        Long generatedUserId = newUser.getUserId();

        //B. local 테이블 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        userDao.saveLocal(generatedUserId, request.getEmail(), encodedPassword);

        //C. user_profile_info 테이블 저장
        userDao.saveProfile(generatedUserId, request.getNickname());

        //메모리에서 인증코드 삭제
        verificationCodeRepository.removeCode(request.getEmail());
    }

    // 프로필 조회 (유저 정보 + 통계 정보)
    public UserProfileDto getProfile(Long userId) {

        // 1. 유저 정보 기본 조회
        UserProfileDto profile = userDao.findProfileByUserId(userId);

        // 2. 통계 정보 조회
        List<SkillStatsDto> skillStats = userDao.getSkillStatistics(userId);

        // 3. 통계 값 세팅
        profile.setSkillStats(skillStats);

        return profile;
    }

    //프로필 수정
    public void updateProfile(Long userId, UserProfileDto.UpdateRequest dto, MultipartFile file) {

        // 기존 DB 정보 조회
        String oldImage = userDao.getProfileImageUrlByUserId(userId);

        // 디폴트는 기존 이름
        String newImage = oldImage;

        // 새 파일이 있는 경우
        if(file != null && !file.isEmpty()) {
            try {
                // 새 파일 물리 저장
                newImage = fileStore.storeFile(file);

                // 기존의 물리 파일 삭제
                if(oldImage != null) {
                    fileStore.deleteFile(oldImage);
                }
            } catch (IOException e) {
                throw new RuntimeException("파일 저장 실패", e);
            }
        }

        // DB 저장
        userDao.updateProfile(userId, dto.getNickname(), newImage, dto.getIntroText());
    }

    // 유저 XP 수정
    @Transactional
    public void updateXp(Long userId, int deltaXp) {
        if (!userDao.existsByUserId(userId)) {
            throw new ResourceNotFoundException("존재하지 않는 사용자");
        }
        Integer currentXp = userDao.findXpByUserId(userId);
        if (currentXp == null) {
            throw new ResourceNotFoundException("사용자 XP 정보 없음");
        }
        int newXp = Math.max(0, currentXp + deltaXp); // XP는 0 이하로 떨어지지 않음

        //DB에 XP 업데이트
        userDao.updateUserXp(userId, newXp);

        //Redis 랭킹 업데이트
        rankingRedisRepository.updateUserScore(userId.toString(), newXp);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long userId, PasswordChangeDto dto) {
        UserEntity user = userDao.findById(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        userDao.updatePassword(userId, encodedNewPassword);
    }

    // 회원 탈퇴
    @Transactional
    public void softDeleteUser(UserRequestDto.Delete dto) {
        // 1. 사용자 정보 조회
        UserEntity user = userDao.findById(dto.getUserId());
        
        if (user == null) {
            throw new ResourceNotFoundException("존재하지 않는 사용자입니다.");
        }
        
        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 3. 회원 탈퇴 처리 (is_deleted = true)
        int updatedRows = userDao.softDeleteUser(dto);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("회원 탈퇴 처리에 실패했습니다.");
        }
    }

    // 비밀번호 초기화 링크 요청
    @Transactional(readOnly = true)
    public void sendResetLink(String email){
        // 가입된 유저인지 확인
        Long userId = userDao.findUserIdByEmail(email);

        if (userId == null) {
            throw new ResourceNotFoundException("해당 이메일로 가입된 계정이 존재하지 않습니다.");
        }

        // 토큰 생성
        String token = UUID.randomUUID().toString();

        // Repository를 통해 저장
        passwordResetRepository.saveToken(token, userId);

        // 메일 보내기
        emailSender.sendPasswordResetEmail(email, token);
    }

    // 토큰 검증 및 비밀번호 실제 변경
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 토큰을 통한 userId 조회
        String userIdStr = passwordResetRepository.getUserId(token);

        if (userIdStr == null) {
            throw new ResourceNotFoundException("유효하지 않거나 만료된 링크입니다.");
        }

        Long userId = Long.parseLong(userIdStr);

        // 유저 존재 확인
        if (!userDao.existsByUserId(userId)) {
            throw new ResourceNotFoundException("존재하지 않는 사용자입니다.");
        }

        // 비밀번호 재설정
        int result = userDao.updatedPassword(userId, passwordEncoder.encode(newPassword));

        if (result == 0) {
            throw new RuntimeException("비밀번호 변경에 실패했습니다.");
        }

        // Repository를 통해 토큰 삭제
        passwordResetRepository.deleteToken(token);
    }

    // 토큰 유효성 체크 : 링크에 진입할 때 검증
    @Transactional(readOnly = true)
    public void validateToken(String token) {
        if (!passwordResetRepository.existsToken(token)) {
            throw new ResourceNotFoundException("유효하지 않거나 만료된 링크입니다.");
        }
    }
}
