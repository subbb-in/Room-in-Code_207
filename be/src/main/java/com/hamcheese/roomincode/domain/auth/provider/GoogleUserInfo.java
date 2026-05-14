package com.hamcheese.roomincode.domain.auth.provider;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo{

    private Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Provider getProvider() { return Provider.GOOGLE; }

    @Override
    // Google sub = String
    public String getProviderId() { return String.valueOf(attributes.get("sub")); }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() { return (String) attributes.get("name"); }
}
