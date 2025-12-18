package com.lifeai.controller;

import com.lifeai.dto.CreateEventRequest;
import com.lifeai.dto.EventResponse;
import com.lifeai.entity.EventStatus;
import com.lifeai.entity.EventType;
import com.lifeai.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EventResponse>> getEventsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(eventService.getEventsByUserId(userId));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<EventResponse>> getEventsByUserAndType(
            @PathVariable Long userId,
            @PathVariable EventType type) {
        return ResponseEntity.ok(eventService.getEventsByUserIdAndType(userId, type));
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<EventResponse>> getEventsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(eventService.getEventsByUserIdAndDateRange(userId, startTime, endTime));
    }

    @GetMapping("/user/{userId}/pending-clarifications")
    public ResponseEntity<List<EventResponse>> getPendingClarifications(@PathVariable Long userId) {
        return ResponseEntity.ok(eventService.getEventsByUserIdAndStatus(userId, EventStatus.NEED_CLARIFICATION));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
