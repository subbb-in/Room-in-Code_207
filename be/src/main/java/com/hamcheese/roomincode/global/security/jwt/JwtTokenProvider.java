package com.hamcheese.roomincode.global.security.jwt;

import com.hamcheese.roomincode.global.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secretkey}")
    private String secretKey;

    private Key key;

    // AccessToken, RefreshToken 사용
    // (Null 방지 위해 long 사용)
    private final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30; // 30분
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 14; // 14일

    public long getRefreshTokenValidityInSeconds() {
        return REFRESH_TOKEN_VALIDITY / 1000; // 쿠키-초단위 / 자바-밀리초 단위이므로 나누기 1000 필요
    }

    @PostConstruct
    protected void init() {
        // 이미 base64로 된 키라 바로 디코딩함(secretKey를 그대로 쓰는 게 아니라 Key 객체로 변환해야 함)
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성
    public String createAccessToken(Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Claims claims = Jwts.claims().setSubject(userDetails.getUsername()); // email
        claims.put("userId", userDetails.getUserId()); // AccessToken 생성 시 userId 포함시킴

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    // 소셜로그인은 CustomOAuth2User가 들어와서 Authentication 객체 대신 userId, email 받기
    // Authentication 없이 값만 받아서 생성
    public String createAccessToken(Long userId, String email) {
        Claims claims = Jwts.claims().setSubject(email); // subject에 이메일 저장
        claims.put("userId", userId);                    // userId 저장

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 소셜로그인용 RefreshToken 생성
    public String createRefreshToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Claims claims = Jwts.claims().setSubject(userDetails.getUsername()); // email
        claims.put("userId", userDetails.getUserId());

        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("userId", userId);

        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("validateToken: 토큰 만료");
        } catch (UnsupportedJwtException e) {
            log.info("validateToken: 지원하지 않는 토큰");
        } catch (MalformedJwtException e) {
            log.info("validateToken: 잘못된 토큰 형식");
        } catch (SignatureException e) {
            log.info("validateToken: 서명 불일치");
        } catch (IllegalArgumentException e) {
            log.info("validateToken: 토큰 값 없음");
        }
        return false;
    }

    // 토큰에서 userId 추출
    public long getUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Number.class).longValue();
    }

    // 토근에서 이메일 추출
    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    // 인증 객체 생성 (토큰에서 인증 정보 조회-Filter에서 사용)
    public Authentication getAuthentication(String token) {
        String email = getEmail(token);
        long userId = getUserId(token);

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, "");

        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities());
    }

    // 남은 만료시간
    public long getRemainTime(String token) {
        Date expiration = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().getExpiration();

        return expiration.getTime() - System.currentTimeMillis();
    }
}