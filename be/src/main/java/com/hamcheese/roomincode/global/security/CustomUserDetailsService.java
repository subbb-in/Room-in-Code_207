package com.hamcheese.roomincode.global.security;

import com.hamcheese.roomincode.domain.user.dao.UserDao;
import com.hamcheese.roomincode.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email for security: {}", email);
        UserEntity user = userDao.findByEmail(email);
        
        if (user == null) {
            log.warn("User not found in database: {}", email);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
        }

        log.info("User found in DB. UserId: {}, Encoded Password Length: {}", user.getUserId(), 
                (user.getPassword() != null ? user.getPassword().length() : "NULL"));

        return new CustomUserDetails(
                user.getUserId(),
                user.getEmail(),
                user.getPassword()
        );
    }
}
