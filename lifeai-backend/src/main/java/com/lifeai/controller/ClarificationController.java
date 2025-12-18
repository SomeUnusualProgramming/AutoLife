package com.lifeai.controller;

import com.lifeai.dto.AiAgentResponse;
import com.lifeai.dto.ClarificationResponseDto;
import com.lifeai.entity.ClarificationSession;
import com.lifeai.service.AiAgentService;
import com.lifeai.service.ClarificationSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/clarification")
@RequiredArgsConstructor
public class ClarificationController {

    private final AiAgentService aiAgentService;
    private final ClarificationSessionService sessionService;

    @PostMapping("/{sessionId}/respond")
    public ResponseEntity<AiAgentResponse> respondToClarification(
            @PathVariable UUID sessionId,
            @RequestBody ClarificationResponseDto response) {
        
        log.info("Processing clarification response for session: {}", sessionId);
        
        try {
            AiAgentResponse agentResponse = aiAgentService.processClarificationResponse(
                sessionId, 
                response.getUserResponse()
            );
            
            return ResponseEntity.status(HttpStatus.OK).body(agentResponse);
        } catch (Exception e) {
            log.error("Error processing clarification response", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AiAgentResponse.builder()
                    .status("ERROR")
                    .aiResponse("Błąd przetwarzania odpowiedzi. Spróbuj ponownie.")
                    .build());
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionContext(@PathVariable UUID sessionId) {
        log.info("Fetching session context for: {}", sessionId);
        
        try {
            Map<String, Object> context = sessionService.getSessionContext(sessionId);
            return ResponseEntity.status(HttpStatus.OK).body(context);
        } catch (Exception e) {
            log.error("Error fetching session context", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Session not found"));
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<ClarificationSession>> getActiveSessions(@PathVariable Long userId) {
        log.info("Fetching active sessions for user: {}", userId);
        
        try {
            List<ClarificationSession> sessions = sessionService.getActiveSessions(userId);
            return ResponseEntity.status(HttpStatus.OK).body(sessions);
        } catch (Exception e) {
            log.error("Error fetching active sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
