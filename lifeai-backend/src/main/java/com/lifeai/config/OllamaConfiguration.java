package com.lifeai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@Getter
public class OllamaConfiguration {

    @Value("${ollama.service.url:http://localhost:11434}")
    private String ollamaServiceUrl;

    @Value("${ollama.model:mistral}")
    private String ollamaModel;

    @Value("${ollama.timeout.seconds:10}")
    private long timeoutSeconds;

    @Bean
    public RestTemplate ollamaRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }
}
