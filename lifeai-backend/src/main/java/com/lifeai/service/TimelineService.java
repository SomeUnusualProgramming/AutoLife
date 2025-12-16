package com.lifeai.service;

import com.lifeai.dto.TimelineDayResponse;
import com.lifeai.dto.TimelineEventResponse;
import com.lifeai.dto.TimelineResponse;
import com.lifeai.entity.Event;
import com.lifeai.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineService {

    private final EventRepository eventRepository;

    public TimelineResponse getTimeline(Long userId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);

        List<Event> events = eventRepository.findByUserIdAndTimestampBetween(
            userId,
            startDateTime,
            endDateTime
        );

        Map<LocalDate, List<TimelineEventResponse>> eventsByDate = events.stream()
            .collect(Collectors.groupingBy(
                event -> event.getTimestamp().toLocalDate(),
                Collectors.mapping(
                    this::toTimelineEventResponse,
                    Collectors.toList()
                )
            ));

        List<TimelineDayResponse> days = eventsByDate.entrySet().stream()
            .map(entry -> TimelineDayResponse.builder()
                .date(entry.getKey())
                .events(entry.getValue().stream()
                    .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
                    .collect(Collectors.toList()))
                .eventCount(entry.getValue().size())
                .build())
            .sorted((d1, d2) -> d2.getDate().compareTo(d1.getDate()))
            .collect(Collectors.toList());

        return TimelineResponse.builder()
            .userId(userId)
            .fromDate(fromDate)
            .toDate(toDate)
            .totalEvents(events.size())
            .days(days)
            .build();
    }

    private TimelineEventResponse toTimelineEventResponse(Event event) {
        return TimelineEventResponse.builder()
            .id(event.getId())
            .type(event.getType())
            .description(event.getDescription())
            .timestamp(event.getTimestamp())
            .metadata(event.getMetadata())
            .build();
    }
}
