package com.hamcheese.roomincode.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로 허용
                .allowedOrigins("http://localhost:3000") // 프론트 주소
                .allowedOrigins("http://localhost:5173") // 프론트 주소
                .allowedOrigins("http://localhost:4443")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true); // 쿠키/Authorization 헤더 허용
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/** 경로로 들어오는 요청을 실제 파일 시스템의 uploadDir로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir);
    }
}