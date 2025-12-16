package com.lifeai.dto;

import com.lifeai.entity.NotificationStatus;
import com.lifeai.entity.NotificationType;
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
public class NotificationResponse {

    private Long id;

    private Long userId;

    private Long calendarId;

    private String title;

    private String message;

    private LocalDateTime scheduledTime;

    private LocalDateTime sentAt;

    private NotificationStatus status;

    private NotificationType type;

    private Integer minutesBefore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
