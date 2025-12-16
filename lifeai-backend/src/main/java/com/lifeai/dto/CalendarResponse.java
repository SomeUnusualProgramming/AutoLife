package com.lifeai.dto;

import com.lifeai.entity.CalendarType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {

    private Long id;

    private Long userId;

    private Long eventId;

    private String title;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private CalendarType type;

    private Boolean reminder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
