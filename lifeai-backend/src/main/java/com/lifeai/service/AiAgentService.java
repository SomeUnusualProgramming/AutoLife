package com.lifeai.service;

import com.lifeai.dto.AiAgentRequest;
import com.lifeai.dto.AiAgentResponse;
import com.lifeai.dto.EventResponse;
import com.lifeai.dto.LlmAnalysisDto;
import com.lifeai.dto.PendingEventDto;
import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.PendingClarificationEvent;
import com.lifeai.entity.enums.ClarificationStatus;
import com.lifeai.repository.PendingClarificationEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AiAgentService {

    private static final float CONFIDENCE_COMPLETE_THRESHOLD = 0.9f;
    private static final float CONFIDENCE_CLARIFY_THRESHOLD = 0.7f;
    private static final int MAX_CLARIFICATION_ROUNDS = 3;

    private final OllamaIntegrationService ollamaService;
    private final PendingEventService pendingEventService;
    private final ClarificationSessionService sessionService;
    private final EventService eventService;
    private final PendingClarificationEventRepository pendingEventRepository;

    public AiAgentService(OllamaIntegrationService ollamaService,
                         PendingEventService pendingEventService,
                         ClarificationSessionService sessionService,
                         EventService eventService,
                         PendingClarificationEventRepository pendingEventRepository) {
        this.ollamaService = ollamaService;
        this.pendingEventService = pendingEventService;
        this.sessionService = sessionService;
        this.eventService = eventService;
        this.pendingEventRepository = pendingEventRepository;
    }

    @Transactional
    public AiAgentResponse analyzeUserInput(AiAgentRequest request) {
        String userInput = request.getTextInput() != null ? request.getTextInput() : request.getTranscribedText();

        if (userInput == null || userInput.trim().isEmpty()) {
            return AiAgentResponse.builder()
                .status("ERROR")
                .action("IGNORE")
                .aiResponse("Nie rozumiem. Proszę spróbować ponownie.")
                .build();
        }

        Map<String, Object> context = buildHistoricalContext(request.getUserId());
        LlmAnalysisDto llmResult = ollamaService.callLlmForAnalysis(userInput, context);

        if (llmResult == null) {
            return AiAgentResponse.builder()
                .status("ERROR")
                .action("IGNORE")
                .aiResponse("Błąd przetwarzania. Spróbuj ponownie.")
                .build();
        }

        Float confidence = llmResult.getConfidence() != null ? llmResult.getConfidence() : 0.5f;

        if (confidence >= CONFIDENCE_COMPLETE_THRESHOLD) {
            return handleCompleteEvent(request.getUserId(), llmResult);
        } else {
            return handleIncompleteEvent(request.getUserId(), llmResult);
        }
    }

    @Transactional
    public AiAgentResponse processClarificationResponse(UUID sessionId, String userResponse) {
        PendingClarificationEvent pending = pendingEventRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        int currentRound = pending.getClarificationRound() + 1;

        Map<String, Object> eventContext = buildEventContext(pending);
        LlmAnalysisDto llmResult = ollamaService.callLlmForClarification(userResponse, eventContext);

        pending.setClarificationRound(currentRound);
        pending.setParsedData(llmResult.getParsedData());
        pending.setConfidenceScore(llmResult.getConfidence());

        Float confidence = llmResult.getConfidence() != null ? llmResult.getConfidence() : 0.5f;

        if (confidence >= CONFIDENCE_COMPLETE_THRESHOLD) {
            pending.setStatus(ClarificationStatus.COMPLETE);
            pendingEventRepository.save(pending);

            Event createdEvent = convertToEvent(pending);
            Event savedEvent = eventService.createEvent(pending.getUserId(), createdEvent);

            sessionService.finalizeSession(sessionId);

            return AiAgentResponse.builder()
                .status("COMPLETE")
                .action("CREATE_EVENT")
                .event(convertEventToResponse(savedEvent))
                .sessionId(sessionId)
                .aiResponse(llmResult.getAiResponse() != null ? llmResult.getAiResponse() : "Event saved!")
                .build();
        } else if (currentRound >= MAX_CLARIFICATION_ROUNDS) {
            pending.setStatus(ClarificationStatus.INCOMPLETE_AUTO_SAVED);
            pending.setClarificationRound(currentRound);
            pendingEventRepository.save(pending);

            Event autoSavedEvent = convertToEvent(pending);
            Event savedEvent = eventService.createEvent(pending.getUserId(), autoSavedEvent);

            sessionService.finalizeSession(sessionId);

            List<String> warnings = new ArrayList<>();
            warnings.add("incomplete_event");
            warnings.add("auto_saved_after_3_rounds");

            return AiAgentResponse.builder()
                .status("INCOMPLETE_AUTO_SAVED")
                .action("CREATE_EVENT")
                .event(convertEventToResponse(savedEvent))
                .sessionId(sessionId)
                .aiResponse("Zapisałem event z dostępnymi danymi. Możesz go edytować później.")
                .warningFlags(warnings)
                .build();
        } else {
            pending.setClarificationRound(currentRound);
            pendingEventRepository.save(pending);

            sessionService.addQaRound(sessionId, "System question", userResponse, llmResult.getParsedData());

            return AiAgentResponse.builder()
                .status("PENDING_CLARIFICATION")
                .action("ASK_QUESTION")
                .sessionId(sessionId)
                .clarificationRound(currentRound)
                .clarificationQuestion(llmResult.getClarificationQuestion())
                .pendingEvent(convertPendingEventToDto(pending))
                .aiResponse(llmResult.getAiResponse())
                .build();
        }
    }

    private AiAgentResponse handleCompleteEvent(Long userId, LlmAnalysisDto llmResult) {
        Event event = Event.builder()
            .userId(userId)
            .type(parseEventType(llmResult.getEventType()))
            .description(extractDescription(llmResult.getParsedData()))
            .timestamp(extractTimestamp(llmResult.getParsedData()))
            .metadata(llmResult.getParsedData() != null ? llmResult.getParsedData() : new HashMap<>())
            .build();

        Event savedEvent = eventService.createEvent(userId, event);

        return AiAgentResponse.builder()
            .status("COMPLETE")
            .action("CREATE_EVENT")
            .event(convertEventToResponse(savedEvent))
            .aiResponse(llmResult.getAiResponse() != null ? llmResult.getAiResponse() : "Event saved!")
            .confidence(llmResult.getConfidence())
            .build();
    }

    private AiAgentResponse handleIncompleteEvent(Long userId, LlmAnalysisDto llmResult) {
        UUID sessionId = UUID.randomUUID();

        PendingClarificationEvent pending = PendingClarificationEvent.builder()
            .userId(userId)
            .sessionId(sessionId)
            .eventType(llmResult.getEventType() != null ? llmResult.getEventType() : "OTHER")
            .rawInput(llmResult.toString())
            .parsedData(llmResult.getParsedData())
            .confidenceScore(llmResult.getConfidence())
            .status(ClarificationStatus.PENDING_CLARIFICATION)
            .clarificationRound(0)
            .build();

        pendingEventRepository.save(pending);
        sessionService.createSession(userId, pending.getId());

        return AiAgentResponse.builder()
            .status("PENDING_CLARIFICATION")
            .action("ASK_QUESTION")
            .sessionId(sessionId)
            .clarificationRound(1)
            .clarificationQuestion(llmResult.getClarificationQuestion())
            .pendingEvent(convertPendingEventToDto(pending))
            .aiResponse(llmResult.getAiResponse())
            .confidence(llmResult.getConfidence())
            .build();
    }

    private Event convertToEvent(PendingClarificationEvent pending) {
        return Event.builder()
            .userId(pending.getUserId())
            .type(parseEventType(pending.getEventType()))
            .description(extractDescription(pending.getParsedData()))
            .timestamp(extractTimestamp(pending.getParsedData()))
            .metadata(pending.getParsedData() != null ? pending.getParsedData() : new HashMap<>())
            .build();
    }

    private EventType parseEventType(String eventTypeStr) {
        try {
            return EventType.valueOf(eventTypeStr.toUpperCase());
        } catch (Exception e) {
            return EventType.OTHER;
        }
    }

    private String extractDescription(Map<String, Object> data) {
        if (data == null) return "No description";
        if (data.containsKey("description")) {
            return data.get("description").toString();
        }
        return data.toString();
    }

    private LocalDateTime extractTimestamp(Map<String, Object> data) {
        if (data != null && data.containsKey("timestamp")) {
            try {
                return LocalDateTime.parse(data.get("timestamp").toString());
            } catch (Exception e) {
                log.warn("Failed to parse timestamp from data", e);
            }
        }
        return LocalDateTime.now();
    }

    private Map<String, Object> buildHistoricalContext(Long userId) {
        try {
            List<Event> recentEvents = eventService.getUserEvents(userId)
                .stream()
                .limit(5)
                .toList();

            Map<String, Object> context = new HashMap<>();
            context.put("recent_events_count", recentEvents.size());
            context.put("recent_events", recentEvents.stream()
                .map(e -> Map.of(
                    "type", e.getType().toString(),
                    "description", e.getDescription(),
                    "timestamp", e.getTimestamp().toString()
                ))
                .toList());

            return context;
        } catch (Exception e) {
            log.warn("Failed to build historical context", e);
            return new HashMap<>();
        }
    }

    private Map<String, Object> buildEventContext(PendingClarificationEvent pending) {
        Map<String, Object> context = new HashMap<>();
        context.put("event_type", pending.getEventType());
        context.put("raw_input", pending.getRawInput());
        context.put("parsed_data", pending.getParsedData());
        context.put("round", pending.getClarificationRound());
        return context;
    }

    private PendingEventDto convertPendingEventToDto(PendingClarificationEvent pending) {
        return PendingEventDto.builder()
            .id(pending.getId())
            .sessionId(pending.getSessionId())
            .type(pending.getEventType())
            .rawInput(pending.getRawInput())
            .metadata(pending.getParsedData())
            .confidence(pending.getConfidenceScore())
            .status(pending.getStatus().toString())
            .clarificationRound(pending.getClarificationRound())
            .build();
    }

    private EventResponse convertEventToResponse(Event event) {
        return EventResponse.builder()
            .id(event.getId())
            .userId(event.getUserId())
            .type(event.getType())
            .description(event.getDescription())
            .timestamp(event.getTimestamp())
            .metadata(event.getMetadata())
            .build();
    }
}
