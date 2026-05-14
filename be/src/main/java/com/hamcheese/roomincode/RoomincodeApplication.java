package com.hamcheese.roomincode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@MapperScan("com.hamcheese.roomincode.domain")
@EnableAsync
public class RoomincodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomincodeApplication.class, args);
    }

}
