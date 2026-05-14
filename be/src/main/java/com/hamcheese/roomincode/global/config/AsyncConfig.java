package com.hamcheese.roomincode.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AsyncConfig {

    @Bean
    public SimpleAsyncTaskScheduler taskScheduler() {
        // 플랫폼 스레드만 사용할 경우
        // return new SimpleAsyncTaskScheduler();

        // Virtual Thread 활성화
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setTaskTerminationTimeout(30000);
        return scheduler;
    }
}
