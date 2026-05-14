package com.hamcheese.roomincode.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String email;
    private String password;

    public CustomUserDetails(Long userId, String email, String password) {
        this.userId = userId;
        this.email = email;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public String getPassword() {
        return password;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // or ROLE_USER
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
