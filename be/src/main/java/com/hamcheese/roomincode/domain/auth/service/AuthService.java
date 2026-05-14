package com.hamcheese.roomincode.domain.auth.service;

import com.hamcheese.roomincode.domain.auth.dto.AuthRequestDto;
import com.hamcheese.roomincode.domain.auth.dto.AuthResponseDto;
import com.hamcheese.roomincode.domain.auth.repository.AuthRedisRepository;
import com.hamcheese.roomincode.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisRepository authRedisRepository;

    @Transactional
    public AuthResponseDto.TokenInfo login(AuthRequestDto.Login requestDto) {
        // 1. 인증 처리 로직
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(

                        requestDto.getEmail(),
                        requestDto.getPassword())
        );

        // 2. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // redis에 RefreshToken 저장
        authRedisRepository.saveRefreshToken(
//                requestDto.getEmail(),
                authentication.getName(),
                refreshToken,
                jwtTokenProvider.getRemainTime(refreshToken)
        );

        // 3. 이너 클래스 생성 및 반환
        return AuthResponseDto.TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void logout(String accessToken) {

        // 로그아웃하고 싶은 토큰이 유효한지 우선 검증
        if(!jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("로그아웃 : 유효하지 않은 토큰입니다.");
        }

        String email = jwtTokenProvider.getEmail(accessToken);

        // RefreshToken 삭제
        authRedisRepository.deleteRefreshToken(email);

        // AccessToken 유효시간 가지고 - 블랙리스트 등록
        long expiration = jwtTokenProvider.getRemainTime(accessToken);
        authRedisRepository.blacklistAccessToken(accessToken, expiration);

    }


    // 토큰 재발급
    public String reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 RefreshToken입니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String savedRefreshToken = authRedisRepository.getRefreshToken(email);

        if(!refreshToken.equals(savedRefreshToken)) {
            throw new RuntimeException("RefreshToken이 유효하지 않습니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        return jwtTokenProvider.createAccessToken(authentication);
    }
//    @Transactional
//    public void logout(TokenRequestDto tokenRequestDto){
//        // 로그아웃 하고 싶은 토큰이 유효한 지 먼저 검증하기
//        if (!jwtTokenProvider.validateToken(tokenRequestDto.getAccessToken())){
//            throw new IllegalArgumentException("로그아웃 : 유효하지 않은 토큰입니다.");
//        }
//
//        // Access Token에서 User email을 가져온다
//        Authentication authentication = jwtTokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
//
//        // Redis에서 해당 User email로 저장된 Refresh Token 이 있는지 여부를 확인 후에 있을 경우 삭제를 한다.
//        if (redisTemplate.opsForValue().get("RT:"+authentication.getName())!=null){
//            // Refresh Token을 삭제
//            redisTemplate.delete("RT:"+authentication.getName());
//        }
//
//        // 해당 Access Token 유효시간을 가지고 와서 BlackList에 저장하기
//        Long expiration = jwtTokenProvider.getExpiration(tokenRequestDto.getAccessToken());
//        redisTemplate.opsForValue().set(tokenRequestDto.getAccessToken(),"logout",expiration,TimeUnit.MILLISECONDS);
//
//    }

}