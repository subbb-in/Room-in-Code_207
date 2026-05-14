package com.hamcheese.roomincode.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth.email")
public class EmailConfig {
    private long expirationMinutes;
    private String subjectPrefix;
}
