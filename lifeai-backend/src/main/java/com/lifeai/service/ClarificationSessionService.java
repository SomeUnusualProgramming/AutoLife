package com.lifeai.service;

import com.lifeai.entity.ClarificationQaRound;
import com.lifeai.entity.ClarificationSession;
import com.lifeai.entity.enums.SessionStatus;
import com.lifeai.repository.ClarificationQaRoundRepository;
import com.lifeai.repository.ClarificationSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ClarificationSessionService {

    private final ClarificationSessionRepository sessionRepository;
    private final ClarificationQaRoundRepository qaRoundRepository;

    public ClarificationSessionService(ClarificationSessionRepository sessionRepository,
                                       ClarificationQaRoundRepository qaRoundRepository) {
        this.sessionRepository = sessionRepository;
        this.qaRoundRepository = qaRoundRepository;
    }

    @Transactional
    public ClarificationSession createSession(Long userId, Long pendingEventId) {
        ClarificationSession session = ClarificationSession.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .pendingEventId(pendingEventId)
            .conversationContext(new HashMap<>())
            .totalRounds(0)
            .status(SessionStatus.ACTIVE)
            .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public ClarificationQaRound addQaRound(UUID sessionId, String question, String userResponse, Map<String, Object> analysis) {
        ClarificationSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        Integer roundNumber = session.getTotalRounds() + 1;

        ClarificationQaRound qaRound = ClarificationQaRound.builder()
            .session(session)
            .roundNumber(roundNumber)
            .aiQuestion(question)
            .userResponse(userResponse)
            .aiAnalysis(analysis)
            .build();

        ClarificationQaRound saved = qaRoundRepository.save(qaRound);

        session.setTotalRounds(roundNumber);
        session.getQaRounds().add(saved);
        sessionRepository.save(session);

        return saved;
    }

    public Map<String, Object> getSessionContext(UUID sessionId) {
        ClarificationSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        Map<String, Object> context = new HashMap<>();
        context.put("session_id", sessionId.toString());
        context.put("total_rounds", session.getTotalRounds());
        context.put("status", session.getStatus().toString());

        List<Map<String, Object>> qaHistory = session.getQaRounds().stream()
            .map(qa -> Map.of(
                "round_number", (Object) qa.getRoundNumber(),
                "question", qa.getAiQuestion(),
                "response", qa.getUserResponse(),
                "analysis", qa.getAiAnalysis()
            ))
            .map(m -> (Map<String, Object>) m)
            .toList();

        context.put("qa_history", qaHistory);
        context.putAll(session.getConversationContext() != null ? session.getConversationContext() : new HashMap<>());

        return context;
    }

    @Transactional
    public void finalizeSession(UUID sessionId) {
        ClarificationSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Session {} finalized", sessionId);
    }

    public List<ClarificationSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE);
    }

    public List<ClarificationSession> getAllUserSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }
}
