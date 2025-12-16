package com.lifeai.repository;

import com.lifeai.entity.Notification;
import com.lifeai.entity.NotificationStatus;
import com.lifeai.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByScheduledTimeDesc(Long userId);

    List<Notification> findByStatusOrderByScheduledTimeAsc(NotificationStatus status);

    List<Notification> findByUserIdAndStatusOrderByScheduledTimeAsc(Long userId, NotificationStatus status);

    List<Notification> findByStatusAndScheduledTimeLessThanEqual(NotificationStatus status, LocalDateTime dateTime);

    List<Notification> findByCalendarIdAndStatus(Long calendarId, NotificationStatus status);

    List<Notification> findByUserIdAndTypeOrderByScheduledTimeDesc(Long userId, NotificationType type);
}
