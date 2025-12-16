package com.lifeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifeAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeAiBackendApplication.class, args);
    }
}
