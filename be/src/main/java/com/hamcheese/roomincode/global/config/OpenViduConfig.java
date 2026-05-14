package com.hamcheese.roomincode.global.config;

import io.openvidu.java.client.OpenVidu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenViduConfig {

    // OpenVidu 서버 url
    @Value("${OPENVIDU_URL}")
    private String openViduUrl;
    // 오픈 비두 서버의 인증 비밀번호
    @Value("${OPENVIDU_SECRET}")
    private String openViduSecret;

    // 오픈 비두 객체 생성
    @Bean
    public OpenVidu openVidu() {
        return new OpenVidu(openViduUrl, openViduSecret);
    }

}
