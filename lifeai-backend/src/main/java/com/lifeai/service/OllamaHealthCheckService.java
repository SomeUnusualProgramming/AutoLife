package com.lifeai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@EnableScheduling
public class OllamaHealthCheckService {

    private final OllamaIntegrationService ollamaIntegrationService;
    private final AtomicBoolean isOllamaAvailable = new AtomicBoolean(true);

    public OllamaHealthCheckService(OllamaIntegrationService ollamaIntegrationService) {
        this.ollamaIntegrationService = ollamaIntegrationService;
        performHealthCheck();
    }

    @Scheduled(fixedRate = 30000)
    public void performHealthCheck() {
        try {
            boolean available = ollamaIntegrationService.isOllamaAvailable();
            boolean wasAvailable = isOllamaAvailable.getAndSet(available);

            if (available && !wasAvailable) {
                log.info("✓ Ollama service is now AVAILABLE - resuming LLM mode");
            } else if (!available && wasAvailable) {
                log.warn("✗ Ollama service is DOWN - falling back to regex detection");
            }
        } catch (Exception e) {
            log.error("Error during Ollama health check", e);
            isOllamaAvailable.set(false);
        }
    }

    public boolean isOllamaHealthy() {
        return isOllamaAvailable.get();
    }

    public void markUnhealthy() {
        if (isOllamaAvailable.getAndSet(false)) {
            log.warn("Ollama marked as unhealthy - fallback to regex");
        }
    }

    public void markHealthy() {
        if (!isOllamaAvailable.getAndSet(true)) {
            log.info("Ollama marked as healthy - resuming LLM mode");
        }
    }
}
