package com.lifeai.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeai.ai.LifeAiService;
import com.lifeai.ai.dto.AiAnalysisRequest;
import com.lifeai.ai.dto.AiAnalysisResponse;
import com.lifeai.ai.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.service.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RestLifeAiServiceClient implements LifeAiService {

    @Value("${ai.service.url:http://localhost:8081}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:30}")
    private int requestTimeout;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    @Override
    public AiAnalysisResponse analyzeHealthData(AiAnalysisRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/api/analyze"))
                    .timeout(Duration.ofSeconds(requestTimeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 400) {
                handleErrorResponse(response);
            }

            return objectMapper.readValue(response.body(), AiAnalysisResponse.class);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error analyzing health data", e);
            throw new AiServiceException(
                    "Failed to analyze health data: " + e.getMessage(),
                    "ANALYSIS_FAILED",
                    500
            );
        }
    }

    @Override
    public String generateRecommendation(String context, String topic) {
        try {
            String requestBody = objectMapper.writeValueAsString(
                    java.util.Map.of("context", context, "topic", topic)
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/api/recommendation"))
                    .timeout(Duration.ofSeconds(requestTimeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 400) {
                handleErrorResponse(response);
            }

            return objectMapper.readValue(response.body(), String.class);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating recommendation", e);
            throw new AiServiceException(
                    "Failed to generate recommendation: " + e.getMessage(),
                    "RECOMMENDATION_FAILED",
                    500
            );
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            boolean available = response.statusCode() == 200;
            log.info("AI Service availability: {}", available);
            return available;
        } catch (Exception e) {
            log.warn("AI Service health check failed", e);
            return false;
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> errorMap = objectMapper.readValue(
                    response.body(),
                    java.util.Map.class
            );
            String errorCode = (String) errorMap.getOrDefault("errorCode", "UNKNOWN");
            String message = (String) errorMap.getOrDefault("message", response.body());

            throw new AiServiceException(message, errorCode, response.statusCode());
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing error response", e);
            throw new AiServiceException(
                    "AI Service error: " + response.statusCode(),
                    "SERVICE_ERROR",
                    response.statusCode()
            );
        }
    }
}
