package com.hamcheese.roomincode.domain.auth.service;

import com.hamcheese.roomincode.domain.auth.provider.*;
import com.hamcheese.roomincode.domain.auth.dto.CustomOAuth2User;
import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserDao userDao;

    @Transactional // DB 작업이 여러 개라 트랜잭션 필수
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 소셜 로그인 API에서 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 어떤 소셜인지 판별
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo;

        if ("google".equals(registrationId)) {
            userInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if ("github".equals(registrationId)) {
            userInfo = new GithubUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider");
        }

        // DB에 저장할 값 추출
        String providerName = userInfo.getProvider().toString();
        String oauthId = userInfo.getProviderId();

        // 3. 이미 가입된 유저인지 social 테이블 확인
        Optional<Long> userIdOptional = userDao.findUserIdBySocial(providerName, oauthId);

        Long userId;
        Map<String, Object> userMap = new HashMap<>();

        if (userIdOptional.isEmpty()) {
            log.info("신규 소셜 회원 가입: {} / {}", providerName, userInfo.getEmail());

            // A. users 테이블 저장 (로그인 타입 SOCIAL)
            UserEntity userEntity = new UserEntity();
            userEntity.setLoginType("SOCIAL");
            userDao.saveUser(userEntity);
            userId = userEntity.getUserId();
//            Long generatedUserId = userEntity.getUserId();

            // B. social 테이블 저장
            userDao.saveSocial(userId, providerName, oauthId);

            // C. user_profile_info 테이블 저장 (초기 닉네임 설정)
            // 닉네임 중복 방지를 위해 소셜닉네임 + 난수 혹은 그냥 소셜닉네임 넣고 나중에 수정 유도
            String initialNickname = userInfo.getNickname();
            userDao.saveProfile(userId, initialNickname);

            // D. 세션/토큰에 담을 정보 구성
            userMap.put("role", "ROLE_USER");
            userMap.put("nickname", initialNickname);

        } else {
            // 기존 로그인
            userId = userIdOptional.get();
            log.info("기존 소셜 회원 로그인: user_id={}", userId);

            // DB에서 현재 닉네임/권한 가져오기 (UserDao에 findProfileByUserId 활용)
            var profile = userDao.findProfileByUserId(userId);
            userMap.put("role", "ROLE_USER");
            userMap.put("nickname", profile.getNickname());
        }

        // 4. CustomOAuth2User 객체 반환 (userId 필수 포함)
        userMap.put("userId", userId);
        userMap.put("email", userInfo.getEmail());

        return new CustomOAuth2User(userMap);
    }
}