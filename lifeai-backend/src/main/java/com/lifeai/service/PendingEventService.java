package com.lifeai.service;

import com.lifeai.entity.Event;
import com.lifeai.entity.PendingClarificationEvent;
import com.lifeai.entity.enums.ClarificationStatus;
import com.lifeai.repository.PendingClarificationEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PendingEventService {

    private final PendingClarificationEventRepository repository;
    private final EventService eventService;

    public PendingEventService(PendingClarificationEventRepository repository, EventService eventService) {
        this.repository = repository;
        this.eventService = eventService;
    }

    @Transactional
    public PendingClarificationEvent createPendingEvent(Long userId, String rawInput, Map<String, Object> parsedData) {
        PendingClarificationEvent pending = PendingClarificationEvent.builder()
            .userId(userId)
            .sessionId(UUID.randomUUID())
            .eventType("OTHER")
            .rawInput(rawInput)
            .parsedData(parsedData)
            .confidenceScore(0.5f)
            .status(ClarificationStatus.PENDING_CLARIFICATION)
            .clarificationRound(0)
            .build();

        return repository.save(pending);
    }

    @Transactional
    public PendingClarificationEvent updatePendingEvent(Long pendingEventId, Map<String, Object> newData) {
        PendingClarificationEvent pending = repository.findById(pendingEventId)
            .orElseThrow(() -> new RuntimeException("Pending event not found"));

        if (newData != null) {
            pending.setParsedData(newData);
        }

        return repository.save(pending);
    }

    public Optional<PendingClarificationEvent> getPendingEventBySessionId(UUID sessionId) {
        return repository.findBySessionId(sessionId);
    }

    @Transactional
    public Event promoteToPermanentEvent(Long pendingEventId) {
        PendingClarificationEvent pending = repository.findById(pendingEventId)
            .orElseThrow(() -> new RuntimeException("Pending event not found"));

        pending.setStatus(ClarificationStatus.COMPLETE);
        repository.save(pending);

        return new Event();
    }

    @Transactional
    public PendingClarificationEvent markAutoSaved(Long pendingEventId, List<String> warningFlags) {
        PendingClarificationEvent pending = repository.findById(pendingEventId)
            .orElseThrow(() -> new RuntimeException("Pending event not found"));

        pending.setStatus(ClarificationStatus.INCOMPLETE_AUTO_SAVED);

        if (pending.getParsedData() == null) {
            pending.setParsedData(new java.util.HashMap<>());
        }
        pending.getParsedData().put("warning_flags", warningFlags);

        return repository.save(pending);
    }

    public List<PendingClarificationEvent> getPendingEventsByUserAndStatus(Long userId, ClarificationStatus status) {
        return repository.findByUserIdAndStatus(userId, status);
    }

    public Optional<PendingClarificationEvent> getLatestPendingEventForUser(Long userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ClarificationStatus.PENDING_CLARIFICATION);
    }
}
