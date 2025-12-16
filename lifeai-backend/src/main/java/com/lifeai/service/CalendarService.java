package com.lifeai.service;

import com.lifeai.dto.CalendarResponse;
import com.lifeai.dto.CreateCalendarRequest;
import com.lifeai.entity.Calendar;
import com.lifeai.entity.CalendarType;
import com.lifeai.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarService {

    private final CalendarRepository calendarRepository;

    public CalendarResponse createCalendarEntry(CreateCalendarRequest request) {
        Calendar calendar = Calendar.builder()
            .userId(request.getUserId())
            .eventId(request.getEventId())
            .title(request.getTitle())
            .description(request.getDescription())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .type(request.getType())
            .reminder(request.getReminder())
            .build();

        Calendar savedCalendar = calendarRepository.save(calendar);
        return toCalendarResponse(savedCalendar);
    }

    @Transactional(readOnly = true)
    public CalendarResponse getCalendarById(Long id) {
        Calendar calendar = calendarRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Calendar entry not found with id: " + id));
        return toCalendarResponse(calendar);
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendarsByUserId(Long userId) {
        return calendarRepository.findByUserIdOrderByStartDateDesc(userId)
            .stream()
            .map(this::toCalendarResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendarsByUserIdAndType(Long userId, CalendarType type) {
        return calendarRepository.findByUserIdAndTypeOrderByStartDateDesc(userId, type)
            .stream()
            .map(this::toCalendarResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendarsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return calendarRepository.findByUserIdAndStartDateBetween(userId, startDate, endDate)
            .stream()
            .map(this::toCalendarResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getCalendarsByUserIdAndDateRangeAndType(Long userId, LocalDateTime startDate, LocalDateTime endDate, CalendarType type) {
        return calendarRepository.findByUserIdAndStartDateBetweenAndType(userId, startDate, endDate, type)
            .stream()
            .map(this::toCalendarResponse)
            .collect(Collectors.toList());
    }

    public CalendarResponse updateCalendar(Long id, CreateCalendarRequest request) {
        Calendar calendar = calendarRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Calendar entry not found with id: " + id));

        calendar.setTitle(request.getTitle());
        calendar.setDescription(request.getDescription());
        calendar.setStartDate(request.getStartDate());
        calendar.setEndDate(request.getEndDate());
        calendar.setType(request.getType());
        calendar.setReminder(request.getReminder());

        Calendar updatedCalendar = calendarRepository.save(calendar);
        return toCalendarResponse(updatedCalendar);
    }

    public void deleteCalendar(Long id) {
        if (!calendarRepository.existsById(id)) {
            throw new IllegalArgumentException("Calendar entry not found with id: " + id);
        }
        calendarRepository.deleteById(id);
    }

    public void createCalendarEntryForMedicalEvent(Long userId, Long eventId, String title, String description, LocalDateTime eventTime) {
        Calendar calendar = Calendar.builder()
            .userId(userId)
            .eventId(eventId)
            .title(title)
            .description(description)
            .startDate(eventTime)
            .endDate(eventTime.plusHours(1))
            .type(CalendarType.MEDICAL)
            .reminder(true)
            .build();

        calendarRepository.save(calendar);
    }

    private CalendarResponse toCalendarResponse(Calendar calendar) {
        return CalendarResponse.builder()
            .id(calendar.getId())
            .userId(calendar.getUserId())
            .eventId(calendar.getEventId())
            .title(calendar.getTitle())
            .description(calendar.getDescription())
            .startDate(calendar.getStartDate())
            .endDate(calendar.getEndDate())
            .type(calendar.getType())
            .reminder(calendar.getReminder())
            .createdAt(calendar.getCreatedAt())
            .updatedAt(calendar.getUpdatedAt())
            .build();
    }
}
