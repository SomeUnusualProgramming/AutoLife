package com.lifeai.service;

import com.lifeai.dto.CreateEventRequest;
import com.lifeai.dto.EventResponse;
import com.lifeai.entity.Event;
import com.lifeai.entity.EventStatus;
import com.lifeai.entity.EventType;
import com.lifeai.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final CalendarService calendarService;

    public EventResponse createEvent(CreateEventRequest request) {
        Event event = Event.builder()
            .userId(request.getUserId())
            .type(request.getType())
            .description(request.getDescription())
            .timestamp(request.getTimestamp())
            .metadata(request.getMetadata())
            .build();

        Event savedEvent = createEventInternal(event);
        return toEventResponse(savedEvent);
    }

    public Event createEvent(Long userId, Event event) {
        event.setUserId(userId);
        return createEventInternal(event);
    }

    private Event createEventInternal(Event event) {
        Event savedEvent = eventRepository.save(event);
        
        if (EventType.MEDICAL == savedEvent.getType() || EventType.DOCTOR_VISIT == savedEvent.getType()) {
            calendarService.createCalendarEntryForMedicalEvent(
                savedEvent.getUserId(),
                savedEvent.getId(),
                "Medical Event: " + savedEvent.getDescription(),
                savedEvent.getDescription(),
                savedEvent.getTimestamp()
            );
        }
        
        return savedEvent;
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
        return toEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByUserId(Long userId) {
        return eventRepository.findByUserIdOrderByTimestampDesc(userId)
            .stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Event> getUserEvents(Long userId) {
        return eventRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByUserIdAndType(Long userId, EventType type) {
        return eventRepository.findByUserIdAndTypeOrderByTimestampDesc(userId, type)
            .stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime)
            .stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByUserIdAndStatus(Long userId, EventStatus status) {
        return eventRepository.findByUserIdAndStatus(userId, status)
            .stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    public EventResponse updateEvent(Long id, CreateEventRequest request) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));

        event.setType(request.getType());
        event.setDescription(request.getDescription());
        event.setTimestamp(request.getTimestamp());
        event.setMetadata(request.getMetadata());

        Event updatedEvent = eventRepository.save(event);
        return toEventResponse(updatedEvent);
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    private EventResponse toEventResponse(Event event) {
        return EventResponse.builder()
            .id(event.getId())
            .userId(event.getUserId())
            .type(event.getType())
            .description(event.getDescription())
            .timestamp(event.getTimestamp())
            .metadata(event.getMetadata())
            .status(event.getStatus())
            .clarificationQuestion(event.getClarificationQuestion())
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }
}
