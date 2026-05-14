package com.hamcheese.roomincode.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${papago.client-id}")
    private String clientId;

    @Value("${papago.client-secret}")
    private String clientSecret;

    @Value("${papago.url}")
    private String baseUrl;

    @Bean
    public WebClient papagoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                // 파파고는 폼 데이터를 주고 받음 -> Content-Type 설정
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                //  모든 요청에 인증 헤더를 포함함
                .defaultHeader("x-ncp-apigw-api-key-id", clientId)
                .defaultHeader("x-ncp-apigw-api-key", clientSecret)
                .build();
    }
}
