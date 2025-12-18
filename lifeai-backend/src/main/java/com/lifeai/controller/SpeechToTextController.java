package com.lifeai.controller;

import com.lifeai.dto.AiAgentRequest;
import com.lifeai.dto.AiAgentResponse;
import com.lifeai.dto.CreateEventRequest;
import com.lifeai.dto.EventResponse;
import com.lifeai.dto.RecommendationResponse;
import com.lifeai.dto.TranscriptionResponse;
import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.service.AiAgentService;
import com.lifeai.service.EventService;
import com.lifeai.service.RecommendationService;
import com.lifeai.service.SpeechToTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/speech-to-text")
@RequiredArgsConstructor
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;
    private final EventService eventService;
    private final RecommendationService recommendationService;
    private final AiAgentService aiAgentService;

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeAudio(
            @RequestParam(value = "file") MultipartFile audioFile,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "userId", required = false) Long userId) {

        long startTime = System.currentTimeMillis();
        
        log.info("Transcribing audio file: {} with language: {}, userId: {}", 
                audioFile.getOriginalFilename(), language, userId);

        String transcribedText = language != null ? 
                ((com.lifeai.service.WhisperSpeechToTextService) speechToTextService).transcribeAudio(audioFile, language) :
                speechToTextService.transcribeAudio(audioFile);

        long processingTime = System.currentTimeMillis() - startTime;

        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            log.warn("Transcription resulted in empty text for file: {}", audioFile.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "No speech detected in audio file. Please ensure the audio contains clear speech."));
        }

        TranscriptionResponse transcriptionResponse = TranscriptionResponse.builder()
                .text(transcribedText)
                .sourceFile(audioFile.getOriginalFilename())
                .language(language)
                .processingTimeMs(processingTime)
                .build();

        log.info("Audio transcription completed in {}ms", processingTime);

        Map<String, Object> response = new HashMap<>();
        response.put("transcription", transcriptionResponse);

        if (userId != null) {
            try {
                AiAgentRequest aiRequest = AiAgentRequest.builder()
                        .userId(userId)
                        .transcribedText(transcribedText)
                        .build();

                AiAgentResponse aiResponse = aiAgentService.analyzeUserInput(aiRequest);
                log.info("AI analysis completed: status={}, action={}", aiResponse.getStatus(), aiResponse.getAction());
                
                if ("CREATE_EVENT".equals(aiResponse.getAction()) && aiResponse.getEvent() != null) {
                    response.put("event", aiResponse.getEvent());
                    log.info("Created event: id={}, userId={}", aiResponse.getEvent().getId(), userId);
                    
                    Event event = new Event();
                    event.setId(aiResponse.getEvent().getId());
                    event.setUserId(userId);
                    event.setType(aiResponse.getEvent().getType());
                    event.setDescription(aiResponse.getEvent().getDescription());
                    event.setTimestamp(aiResponse.getEvent().getTimestamp());
                    
                    List<RecommendationResponse> recommendations = new ArrayList<>();
                    try {
                        recommendations = recommendationService.generateRecommendationsForEvent(event);
                        log.info("Generated {} recommendations for event id: {}", recommendations.size(), aiResponse.getEvent().getId());
                    } catch (Exception e) {
                        log.warn("Failed to generate recommendations for event: {}", e.getMessage());
                    }
                    
                    response.put("recommendations", recommendations);
                } else if ("ASK_QUESTION".equals(aiResponse.getAction())) {
                    response.put("event", aiResponse.getPendingEvent());
                    response.put("clarificationQuestion", aiResponse.getClarificationQuestion());
                    response.put("sessionId", aiResponse.getSessionId());
                    log.info("Pending clarification: sessionId={}, round={}", aiResponse.getSessionId(), aiResponse.getClarificationRound());
                } else {
                    log.warn("Unhandled AI response action: {}", aiResponse.getAction());
                }
                
            } catch (Exception e) {
                log.error("Failed to process transcription with AI agent: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to process event: " + e.getMessage()));
            }
        } else {
            log.warn("userId not provided in transcribe request");
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/transcribe-bytes")
    public ResponseEntity<Map<String, Object>> transcribeAudioBytes(
            @RequestBody byte[] audioBytes,
            @RequestParam(value = "mimeType") String mimeType,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "userId", required = false) Long userId) {

        long startTime = System.currentTimeMillis();

        log.info("Transcribing audio bytes with mime type: {} and language: {}, userId: {}", 
                mimeType, language, userId);

        String transcribedText = language != null ? 
                ((com.lifeai.service.WhisperSpeechToTextService) speechToTextService).transcribeAudio(audioBytes, mimeType, language) :
                speechToTextService.transcribeAudio(audioBytes, mimeType);

        long processingTime = System.currentTimeMillis() - startTime;

        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            log.warn("Transcription resulted in empty text for mime type: {}", mimeType);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "No speech detected in audio. Please ensure the audio contains clear speech."));
        }

        TranscriptionResponse transcriptionResponse = TranscriptionResponse.builder()
                .text(transcribedText)
                .language(language)
                .processingTimeMs(processingTime)
                .build();

        log.info("Audio transcription completed in {}ms", processingTime);

        Map<String, Object> response = new HashMap<>();
        response.put("transcription", transcriptionResponse);

        if (userId != null) {
            try {
                AiAgentRequest aiRequest = AiAgentRequest.builder()
                        .userId(userId)
                        .transcribedText(transcribedText)
                        .build();

                AiAgentResponse aiResponse = aiAgentService.analyzeUserInput(aiRequest);
                log.info("AI analysis completed: status={}, action={}", aiResponse.getStatus(), aiResponse.getAction());
                
                if ("CREATE_EVENT".equals(aiResponse.getAction()) && aiResponse.getEvent() != null) {
                    response.put("event", aiResponse.getEvent());
                    log.info("Created event: id={}, userId={}", aiResponse.getEvent().getId(), userId);
                    
                    Event event = new Event();
                    event.setId(aiResponse.getEvent().getId());
                    event.setUserId(userId);
                    event.setType(aiResponse.getEvent().getType());
                    event.setDescription(aiResponse.getEvent().getDescription());
                    event.setTimestamp(aiResponse.getEvent().getTimestamp());
                    
                    List<RecommendationResponse> recommendations = new ArrayList<>();
                    try {
                        recommendations = recommendationService.generateRecommendationsForEvent(event);
                        log.info("Generated {} recommendations for event id: {}", recommendations.size(), aiResponse.getEvent().getId());
                    } catch (Exception e) {
                        log.warn("Failed to generate recommendations for event: {}", e.getMessage());
                    }
                    
                    response.put("recommendations", recommendations);
                } else if ("ASK_QUESTION".equals(aiResponse.getAction())) {
                    response.put("event", aiResponse.getPendingEvent());
                    response.put("clarificationQuestion", aiResponse.getClarificationQuestion());
                    response.put("sessionId", aiResponse.getSessionId());
                    log.info("Pending clarification: sessionId={}, round={}", aiResponse.getSessionId(), aiResponse.getClarificationRound());
                } else {
                    log.warn("Unhandled AI response action: {}", aiResponse.getAction());
                }
                
            } catch (Exception e) {
                log.error("Failed to process transcription with AI agent: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to process event: " + e.getMessage()));
            }
        } else {
            log.warn("userId not provided in transcribe-bytes request");
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/process")
    public ResponseEntity<AiAgentResponse> processEventInput(@RequestBody AiAgentRequest request) {
        log.info("Processing event input from user: {}", request.getUserId());
        
        try {
            AiAgentResponse response = aiAgentService.analyzeUserInput(request);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error processing event input", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AiAgentResponse.builder()
                    .status("ERROR")
                    .action("IGNORE")
                    .aiResponse("Błąd przetwarzania. Spróbuj ponownie.")
                    .build());
        }
    }

    private EventType detectEventType(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("jad") || lowerText.contains("jadł") || lowerText.contains("posiłek") || 
            lowerText.contains("sniadani") || lowerText.contains("obiad") || lowerText.contains("kolacj")) {
            return EventType.FOOD;
        } else if (lowerText.contains("aktywno") || lowerText.contains("ćwicz") || lowerText.contains("trening") ||
                   lowerText.contains("spacer") || lowerText.contains("sport")) {
            return EventType.ACTIVITY;
        } else if (lowerText.contains("lekarz") || lowerText.contains("doktor") || lowerText.contains("wizyta")) {
            return EventType.DOCTOR_VISIT;
        } else if (lowerText.contains("lekarst") || lowerText.contains("medycyn") || lowerText.contains("lek ")) {
            return EventType.MEDICATION;
        } else if (lowerText.contains("objaw") || lowerText.contains("ból") || lowerText.contains("gorączk")) {
            return EventType.SYMPTOM;
        } else if (lowerText.contains("wag") || lowerText.contains("kilogram")) {
            return EventType.WEIGHT;
        } else if (lowerText.contains("sen") || lowerText.contains("spał")) {
            return EventType.SLEEP;
        } else if (lowerText.contains("humor") || lowerText.contains("nastrój") || lowerText.contains("czuj")) {
            return EventType.MOOD;
        } else {
            return EventType.OTHER;
        }
    }

    private Map<String, Object> createMetadata(String text) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "speech-to-text");
        metadata.put("textLength", text.length());
        metadata.put("createdAt", LocalDateTime.now());
        return metadata;
    }
}
