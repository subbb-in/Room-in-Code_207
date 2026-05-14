package com.hamcheese.roomincode.domain.auth.provider;

public interface OAuth2UserInfo {
    Provider getProvider();
    String getProviderId();
    String getEmail();
    String getNickname();
}