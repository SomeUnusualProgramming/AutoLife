package com.lifeai.controller;

import com.lifeai.dto.TimelineResponse;
import com.lifeai.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<TimelineResponse> getTimeline(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        LocalDate fromDate = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate toDate = to != null ? to : LocalDate.now().plusDays(1);
        Long userIdResolved = userId != null ? userId : 1L;
        
        return ResponseEntity.ok(timelineService.getTimeline(userIdResolved, fromDate, toDate));
    }
}
