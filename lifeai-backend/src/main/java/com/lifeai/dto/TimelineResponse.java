package com.lifeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineResponse {
    private Long userId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer totalEvents;
    private List<TimelineDayResponse> days;
}
