package com.hamcheese.roomincode.domain.auth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

// 스프링 시큐리티 세션(SecurityContext)에 들어갈 객체
public class CustomOAuth2User implements OAuth2User {

    private final Map<String, Object> userMap; // DB에서 조회한 유저 정보

    public CustomOAuth2User(Map<String, Object> userMap) {
        this.userMap = userMap;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return userMap;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(() -> (String) userMap.get("role")); // ROLE_USER 등
        return collection;
    }

    @Override
    public String getName() {
        return (String) userMap.get("nickname"); // 세션에서 꺼내 쓸 이름
    }

    public Long getUserId() {
        return Long.valueOf(userMap.get("userId").toString());
    }
    public String getEmail() {
        return (String) userMap.get("email");
    }
}