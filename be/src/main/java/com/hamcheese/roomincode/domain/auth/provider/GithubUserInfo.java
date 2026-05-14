package com.hamcheese.roomincode.domain.auth.provider;

import java.util.Map;

public class GithubUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    public GithubUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Provider getProvider() { return Provider.GITHUB; }

    @Override
    // Github id = long
    public String getProviderId() { return String.valueOf(attributes.get("id")); }

    @Override
    public String getEmail() {
        // 깃허브는 이메일 비공개 가능하며 비공개시 null로 들어옴
        // null이면 아이디 임의생성
//        String email = (String) attributes.get("email");
//        if (email == null) {
//            return getProviderId() + "@github.com";
//        }
//        return email;
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() { return (String) attributes.get("login"); } // name 대신 login 사용
}
