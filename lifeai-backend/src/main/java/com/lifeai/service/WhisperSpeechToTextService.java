package com.lifeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Primary
public class WhisperSpeechToTextService implements SpeechToTextService {

    private final RestTemplate restTemplate;
    private final String sttServiceUrl;
    private final String defaultLanguage;
    private final ObjectMapper objectMapper;
    
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 30000;
    private LocalDateTime lastRateLimitTime = null;
    private static final long RATE_LIMIT_COOLDOWN_MS = 60000;

    public WhisperSpeechToTextService(
            RestTemplate restTemplate,
            @Value("${stt.service-url:http://localhost:5000}") String sttServiceUrl,
            @Value("${stt.default-language:pl}") String defaultLanguage,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.sttServiceUrl = sttServiceUrl;
        this.defaultLanguage = defaultLanguage;
        this.objectMapper = objectMapper;
        
        log.info("WhisperSpeechToTextService initialized with STT service URL: {}", sttServiceUrl);
    }

    @Override
    public String transcribeAudio(MultipartFile audioFile) {
        return transcribeAudio(audioFile, defaultLanguage);
    }

    public String transcribeAudio(MultipartFile audioFile, String language) {
        try {
            log.info("Transcribing audio file: {} with language: {}", audioFile.getOriginalFilename(), language);

            byte[] audioBytes = audioFile.getBytes();
            return transcribeAudioBytes(audioBytes, audioFile.getContentType(), language != null ? language : defaultLanguage);
        } catch (IOException e) {
            log.error("Failed to read audio file: {}", audioFile.getOriginalFilename(), e);
            throw new RuntimeException("Failed to read audio file", e);
        }
    }

    @Override
    public String transcribeAudio(byte[] audioBytes, String mimeType) {
        return transcribeAudio(audioBytes, mimeType, defaultLanguage);
    }

    public String transcribeAudio(byte[] audioBytes, String mimeType, String language) {
        return transcribeAudioBytes(audioBytes, mimeType, language != null ? language : defaultLanguage);
    }

    private String transcribeAudioBytes(byte[] audioBytes, String mimeType, String language) {
        validateConfiguration();
        
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        
        while (attempt < MAX_RETRIES) {
            try {
                checkRateLimit();
                
                log.info("Transcribing audio bytes with language: {} using STT service at {} (attempt {}/{})", 
                    language, sttServiceUrl, attempt + 1, MAX_RETRIES);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new org.springframework.core.io.ByteArrayResource(audioBytes) {
                    @Override
                    public String getFilename() {
                        return "audio.webm";
                    }
                });
                body.add("language", language);

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                String transcribeUrl = sttServiceUrl + "/transcribe";
                log.debug("Sending request to: {}", transcribeUrl);

                String response = restTemplate.postForObject(transcribeUrl, requestEntity, String.class);

                JsonNode jsonResponse = objectMapper.readTree(response);
                String transcription = jsonResponse.get("text").asText();

                log.info("Audio transcription completed successfully");
                return transcription;
                
            } catch (HttpClientErrorException e) {
                handleHttpError(e, attempt, MAX_RETRIES);
                
                if (isRetryable(e.getStatusCode().value()) && attempt < MAX_RETRIES - 1) {
                    attempt++;
                    waitBeforeRetry(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                } else {
                    throw new RuntimeException("STT Service Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
                }
            } catch (HttpServerErrorException e) {
                log.error("STT Server Error: status={}, message={}", e.getStatusCode(), e.getResponseBodyAsString());
                if (attempt < MAX_RETRIES - 1) {
                    attempt++;
                    waitBeforeRetry(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                } else {
                    throw new RuntimeException("STT Service Server Error: " + e.getStatusCode(), e);
                }
            } catch (Exception e) {
                log.error("Transcription failed on attempt {}/{}: {}", attempt + 1, MAX_RETRIES, e.getMessage(), e);
                if (attempt < MAX_RETRIES - 1) {
                    attempt++;
                    waitBeforeRetry(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                } else {
                    throw new RuntimeException("Failed to transcribe audio after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("Failed to transcribe audio: max retries exceeded");
    }
    
    private void validateConfiguration() {
        if (sttServiceUrl == null || sttServiceUrl.trim().isEmpty()) {
            log.error("STT service URL is not configured. Set stt.service-url in application.yml.");
            throw new RuntimeException("STT service URL is not configured.");
        }
        
        if (sttServiceUrl.contains("openai")) {
            log.error("Invalid STT service URL: {}. Should point to local STT service, not OpenAI API.", sttServiceUrl);
            throw new RuntimeException("STT service is misconfigured. URL should not point to OpenAI API.");
        }
    }
    
    private void checkRateLimit() throws InterruptedException {
        if (lastRateLimitTime != null) {
            long timeSinceLimit = System.currentTimeMillis() - 
                lastRateLimitTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            if (timeSinceLimit < RATE_LIMIT_COOLDOWN_MS) {
                long waitTime = RATE_LIMIT_COOLDOWN_MS - timeSinceLimit;
                log.warn("Rate limit cooldown active. Waiting {} ms before retry", waitTime);
                Thread.sleep(waitTime);
            }
            lastRateLimitTime = null;
        }
    }
    
    private void handleHttpError(HttpClientErrorException e, int attempt, int maxRetries) {
        String errorBody = e.getResponseBodyAsString();
        int statusCode = e.getStatusCode().value();
        
        if (statusCode == 429 || statusCode == 403 ||
            errorBody.contains("insufficient_quota") || 
            errorBody.contains("quota")) {
            
            log.error("Rate limit or quota exceeded. status={}, message={}", statusCode, errorBody);
            lastRateLimitTime = LocalDateTime.now();
            
            if (attempt >= maxRetries - 1) {
                throw new RuntimeException(
                    "STT Service quota exceeded. Please check your OpenAI API key and billing. " +
                    "If using local STT service, verify STT_SERVICE_URL is configured correctly. " +
                    "Status: " + statusCode, e);
            }
        } else {
            log.error("STT Service HTTP Error: status={}, message={}", statusCode, errorBody);
        }
    }
    
    private boolean isRetryable(int statusCode) {
        return statusCode == 429 ||    // TOO_MANY_REQUESTS
               statusCode == 500 ||    // INTERNAL_SERVER_ERROR
               statusCode == 502 ||    // BAD_GATEWAY
               statusCode == 503 ||    // SERVICE_UNAVAILABLE
               statusCode == 504 ||    // GATEWAY_TIMEOUT
               statusCode == 408;      // REQUEST_TIMEOUT
    }
    
    private void waitBeforeRetry(long backoffMs) {
        try {
            log.info("Waiting {} ms before retry...", backoffMs);
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted", e);
        }
    }
}
