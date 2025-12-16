package com.lifeai.service;

import com.lifeai.dto.CreateNotificationRequest;
import com.lifeai.dto.NotificationResponse;
import com.lifeai.entity.Notification;
import com.lifeai.entity.NotificationStatus;
import com.lifeai.entity.NotificationType;
import com.lifeai.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
            .userId(request.getUserId())
            .calendarId(request.getCalendarId())
            .title(request.getTitle())
            .message(request.getMessage())
            .scheduledTime(request.getScheduledTime())
            .type(request.getType())
            .minutesBefore(request.getMinutesBefore())
            .status(NotificationStatus.PENDING)
            .build();

        Notification savedNotification = notificationRepository.save(notification);
        return toNotificationResponse(savedNotification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
        return toNotificationResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByScheduledTimeDesc(userId)
            .stream()
            .map(this::toNotificationResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getPendingNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndStatusOrderByScheduledTimeAsc(userId, NotificationStatus.PENDING)
            .stream()
            .map(this::toNotificationResponse)
            .collect(Collectors.toList());
    }

    public NotificationResponse updateNotification(Long id, CreateNotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setScheduledTime(request.getScheduledTime());
        notification.setType(request.getType());
        notification.setMinutesBefore(request.getMinutesBefore());

        Notification updatedNotification = notificationRepository.save(notification);
        return toNotificationResponse(updatedNotification);
    }

    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }

    public void markNotificationAsSent(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void markNotificationAsFailed(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);
    }

    public void createNotificationForCalendarEntry(Long userId, Long calendarId, String title, String message, LocalDateTime eventTime, Integer minutesBefore) {
        LocalDateTime scheduledTime = eventTime.minusMinutes(minutesBefore);

        Notification notification = Notification.builder()
            .userId(userId)
            .calendarId(calendarId)
            .title(title)
            .message(message)
            .scheduledTime(scheduledTime)
            .type(NotificationType.REMINDER)
            .minutesBefore(minutesBefore)
            .status(NotificationStatus.PENDING)
            .build();

        notificationRepository.save(notification);
    }

    @Scheduled(fixedRate = 60000)
    public void processPendingNotifications() {
        log.debug("Processing pending notifications");
        LocalDateTime now = LocalDateTime.now();

        List<Notification> pendingNotifications = notificationRepository.findByStatusAndScheduledTimeLessThanEqual(NotificationStatus.PENDING, now);

        for (Notification notification : pendingNotifications) {
            try {
                sendNotification(notification);
                markNotificationAsSent(notification.getId());
                log.info("Notification {} sent successfully", notification.getId());
            } catch (Exception e) {
                log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
                markNotificationAsFailed(notification.getId());
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldNotifications() {
        log.debug("Cleaning up old notifications");
    }

    private void sendNotification(Notification notification) {
        log.info("Sending notification to user {}: {} - {}", 
            notification.getUserId(), 
            notification.getTitle(), 
            notification.getMessage());
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .userId(notification.getUserId())
            .calendarId(notification.getCalendarId())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .scheduledTime(notification.getScheduledTime())
            .sentAt(notification.getSentAt())
            .status(notification.getStatus())
            .type(notification.getType())
            .minutesBefore(notification.getMinutesBefore())
            .createdAt(notification.getCreatedAt())
            .updatedAt(notification.getUpdatedAt())
            .build();
    }
}
