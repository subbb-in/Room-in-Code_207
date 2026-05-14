package com.hamcheese.roomincode.domain.user.dao;

import com.hamcheese.roomincode.domain.user.dto.SkillStatsDto;
import com.hamcheese.roomincode.domain.user.dto.UserProfileDto;
import com.hamcheese.roomincode.domain.user.dto.UserRequestDto;
import com.hamcheese.roomincode.domain.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserDao {

    // userId로 유저의 존재여부 찾기
    boolean existsByUserId(Long userId);

    //닉네임 중복체크
    boolean existsByNickname(@Param("nickname") String nickname);

    //이메일 중복조회(local 테이블 조회)
    boolean existsByEmail(@Param("email") String email);

    //회원가입
    //A. users 테이블 저장, 가장 먼저 실행되어 user_id 생성
    void saveUser(UserEntity userEntity);

    // 기존 사진 정보 찾아오기
    String getProfileImageUrlByUserId(Long userId);

    // local 테이블 저장, 생성된 user_id 사용
    void saveLocal(@Param("userId") Long userId,
                   @Param("email") String email,
                   @Param("password") String password);

    // user_profile_info 테이블 저장, 생성된 user_id 사용
    void saveProfile(@Param("userId") Long userId,
                     @Param("nickname") String nickname);

    UserEntity findById(Long id);

    // 이메일로 유저 찾기
    UserEntity findByEmail(String email);

    // 프로필 조회
    UserProfileDto findProfileByUserId(@Param("userId") Long userId);

    //XP만 조회
    Integer findXpByUserId(@Param("userId") Long userId);

    // 프로필 수정
    void updateProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("profileImageUrl") String profileImageUrl,
                      @Param("introText") String introText);

    // XP 업데이트
    void updateUserXp(@Param("userId") Long userId, @Param("newXp") int newXp);

    // 비밀번호 변경(updateUser)
    void updatePassword(@Param("userId") Long userId, @Param("encodedPassword") String encodedPassword);

    // 회원 삭제 (softDelete)
    int softDeleteUser(UserRequestDto.Delete dto);

    // email로 userId 찾기
    Long findUserIdByEmail(String email);

    // 비밀번호 재설정
    int updatedPassword(@Param("userId") Long userId, @Param("password") String password);

    // 문제 풀이 기록 존재 여부 확인
    boolean existsHistory(@Param("userId") Long userId, @Param("problemId") Long problemId);

    // 문제에 매핑된 스킬 ID 리스트 조회
    List<Long> findSkillIdsByProblemId(@Param("problemId") Long problemId);

    // 문제 풀이 기록 저장
    void saveSolvedHistory(@Param("userId") Long userId,
                           @Param("problemId") Long problemId,
                           @Param("skillIds") List<Long> skillIds);

    // 스킬 통계 데이터 조회
    List<SkillStatsDto> getSkillStatistics(@Param("userId") Long userId);

    // social 테이블 저장
    void saveSocial(@Param("userId") Long userId,
                    @Param("providerName") String providerName,
                    @Param("oauthId") String oauthId);

    // 소셜 정보로 userId 찾기
    Optional<Long> findUserIdBySocial(@Param("providerName") String providerName,
                                      @Param("oauthId") String oauthId);
}
