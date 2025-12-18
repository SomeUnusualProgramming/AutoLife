package com.lifeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeai.config.OllamaConfiguration;
import com.lifeai.dto.LlmAnalysisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OllamaIntegrationService {

    private final OllamaConfiguration config;
    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    public OllamaIntegrationService(OllamaConfiguration config, RestTemplate ollamaRestTemplate) {
        this.config = config;
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public LlmAnalysisDto callLlmForAnalysis(String userInput, Map<String, Object> context) {
        try {
            String prompt = formatPromptWithContext(userInput, context);
            LlmAnalysisDto result = sendPromptToOllama(prompt);
            log.info("✓ Event analysis completed using LLM");
            return result;
        } catch (ResourceAccessException e) {
            log.warn("Ollama service unavailable (timeout/connection), falling back to regex", e);
            return fallbackAnalysis(userInput);
        } catch (RestClientException e) {
            log.warn("Ollama service error, falling back to regex: {}", e.getMessage());
            return fallbackAnalysis(userInput);
        } catch (Exception e) {
            log.error("Unexpected error during LLM analysis, falling back to regex", e);
            return fallbackAnalysis(userInput);
        }
    }

    public LlmAnalysisDto callLlmForClarification(String userInput, Map<String, Object> eventContext) {
        try {
            String prompt = formatClarificationPrompt(userInput, eventContext);
            return sendPromptToOllama(prompt);
        } catch (Exception e) {
            log.error("Error during clarification LLM call", e);
            return LlmAnalysisDto.builder()
                .status("ERROR")
                .clarificationQuestion("Proszę podać więcej szczegółów.")
                .aiResponse("Przepraszam, nie mogę przetworzyć Twojej odpowiedzi.")
                .build();
        }
    }

    public boolean isOllamaAvailable() {
        try {
            String healthUrl = config.getOllamaServiceUrl() + "/api/tags";
            ollamaRestTemplate.getForObject(healthUrl, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Ollama health check failed", e);
            return false;
        }
    }

    private LlmAnalysisDto sendPromptToOllama(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getOllamaModel());
        requestBody.put("prompt", prompt);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Sending prompt to Ollama (model: {}, timeout: {}s)", 
                config.getOllamaModel(), config.getTimeoutSeconds());
            
            String response = ollamaRestTemplate.postForObject(
                config.getOllamaServiceUrl() + "/api/generate",
                entity,
                String.class
            );

            log.debug("Ollama response received successfully");
            return parseOllamaResponse(response);
            
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("Ollama request timed out after {}s - falling back to regex", 
                    config.getTimeoutSeconds(), e);
            } else {
                log.error("Ollama connection failed (ResourceAccessException) - falling back to regex", e);
            }
            throw e;
        } catch (RestClientException e) {
            log.error("Ollama API call failed - falling back to regex: {}", e.getMessage());
            throw e;
        }
    }

    private LlmAnalysisDto parseOllamaResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String fullResponse = root.get("response").asText();

            return parseJsonFromResponse(fullResponse);
        } catch (Exception e) {
            log.error("Failed to parse Ollama response", e);
            return LlmAnalysisDto.builder()
                .status("ERROR")
                .aiResponse("Błąd przetwarzania - spróbuj ponownie")
                .build();
        }
    }

    private LlmAnalysisDto parseJsonFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            return LlmAnalysisDto.builder()
                .status(root.has("status") ? root.get("status").asText() : "COMPLETE")
                .eventType(root.has("event_type") ? root.get("event_type").asText() : "OTHER")
                .parsedData(objectMapper.convertValue(root.get("parsed_data"), Map.class))
                .confidence(root.has("confidence") ? root.get("confidence").floatValue() : 0.5f)
                .clarificationQuestion(root.has("clarification_question") ? root.get("clarification_question").asText() : null)
                .aiResponse(root.has("ai_response") ? root.get("ai_response").asText() : "")
                .build();
        } catch (Exception e) {
            log.error("Error parsing JSON from LLM response", e);
            return LlmAnalysisDto.builder()
                .status("ERROR")
                .build();
        }
    }

    public String formatPromptWithContext(String userInput, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Jesteś asystentem analizy zdarzeń zdrowotnych. Odpowiadaj WYŁĄCZNIE w formacie JSON, bez dodatkowego tekstu.\n\n");
        
        prompt.append("=== Dostępne Typy Zdarzeń ===\n");
        prompt.append("FOOD: Posiłki, przekąski, napoje\n");
        prompt.append("ACTIVITY: Ćwiczenia, sport, aktywność fizyczna\n");
        prompt.append("MEDICATION: Leki, suplementy\n");
        prompt.append("DOCTOR_VISIT: Wizyty lekarskie\n");
        prompt.append("SYMPTOM: Objawy, dolegliwości\n");
        prompt.append("WEIGHT: Pomiary wagi\n");
        prompt.append("SLEEP: Sen, odpoczynek\n");
        prompt.append("MOOD: Nastrój, stan emocjonalny\n");
        prompt.append("OTHER: Inne zdarzenia\n\n");

        if (context != null && !context.isEmpty()) {
            prompt.append("=== Kontekst Historyczny Użytkownika ===\n");
            Integer recentCount = (Integer) context.get("recent_events_count");
            if (recentCount != null && recentCount > 0) {
                prompt.append("Użytkownik ma ").append(recentCount).append(" ostatnich zdarzeń:\n");
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> recentEvents = 
                    (java.util.List<Map<String, Object>>) context.get("recent_events");
                if (recentEvents != null) {
                    for (int i = 0; i < recentEvents.size(); i++) {
                        Map<String, Object> event = recentEvents.get(i);
                        prompt.append(i + 1).append(". ").append(event.get("type"))
                            .append(": ").append(event.get("description"))
                            .append(" (").append(event.get("timestamp")).append(")\n");
                    }
                }
            }
            prompt.append("\n");
        }

        prompt.append("=== Analizuj Wejście Użytkownika ===\n");
        prompt.append("Wejście: \"").append(userInput).append("\"\n\n");

        prompt.append("=== Wytyczne ===\n");
        prompt.append("1. Ekstrahuj WSZYSTKIE dostępne informacje\n");
        prompt.append("2. Timestamp: Format ISO (YYYY-MM-DDTHH:mm:ss), jeśli nie podano - bieżący czas\n");
        prompt.append("3. Confidence: 0.9+ (pełne), 0.7-0.9 (częściowe), <0.7 (wymaga wyjaśnienia)\n");
        prompt.append("4. Jeśli brakuje danych: status=PENDING_CLARIFICATION, zadaj konkretne pytanie\n");
        prompt.append("5. Odpowiadaj naturalnie po polsku\n\n");

        prompt.append("=== Format Odpowiedzi ===\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"COMPLETE\" lub \"PENDING_CLARIFICATION\",\n");
        prompt.append("  \"event_type\": \"TYP_ZDARZENIA\",\n");
        prompt.append("  \"parsed_data\": {\n");
        prompt.append("    \"description\": \"...\",\n");
        prompt.append("    \"timestamp\": \"ISO_DATETIME\",\n");
        prompt.append("    \"duration_minutes\": liczba lub null,\n");
        prompt.append("    \"intensity\": \"low/moderate/high\" lub null,\n");
        prompt.append("    ... (inne pola zależne od typu)\n");
        prompt.append("  },\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"clarification_question\": \"Pytanie lub null\",\n");
        prompt.append("  \"ai_response\": \"Wiadomość po polsku\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String formatClarificationPrompt(String userResponse, Map<String, Object> eventContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Jesteś asystentem wyjaśniania zdarzeń zdrowotnych. Odpowiadaj WYŁĄCZNIE w formacie JSON.\n\n");
        prompt.append("=== Kontekst Poprzedniego Zdarzenia ===\n");
        prompt.append(eventContext).append("\n\n");
        
        prompt.append("=== Odpowiedź Użytkownika ===\n");
        prompt.append("Odpowiedź: \"").append(userResponse).append("\"\n\n");

        prompt.append("=== Wytyczne ===\n");
        prompt.append("1. Integruj nową odpowiedź z poprzednimi danymi\n");
        prompt.append("2. Jeśli odpowiedź uzupełnia brakujące pola - ustaw status=COMPLETE\n");
        prompt.append("3. Jeśli wciąż brakuje danych - zadaj pytanie wyjaśniające\n");
        prompt.append("4. Confidence: 0.9+ (kompletne), <0.9 (potrzeba więcej)\n");
        prompt.append("5. Odpowiadaj po polsku\n\n");

        prompt.append("=== Format Odpowiedzi ===\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"COMPLETE\" lub \"PENDING_CLARIFICATION\",\n");
        prompt.append("  \"parsed_data\": { \"...zaktualizowane pola...\" },\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"clarification_question\": \"Następne pytanie lub null\",\n");
        prompt.append("  \"ai_response\": \"Wiadomość po polsku\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private LlmAnalysisDto fallbackAnalysis(String userInput) {
        log.warn("⚠ Using regex fallback for event analysis (Ollama unavailable)");
        return LlmAnalysisDto.builder()
            .status("FALLBACK_REGEX")
            .eventType("OTHER")
            .confidence(0.5f)
            .aiResponse("Nie mogę połączyć się z usługą AI. Używam podstawowego przetwarzania.")
            .build();
    }
}
