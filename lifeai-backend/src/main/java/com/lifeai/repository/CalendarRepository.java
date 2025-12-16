package com.lifeai.repository;

import com.lifeai.entity.Calendar;
import com.lifeai.entity.CalendarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    List<Calendar> findByUserIdOrderByStartDateDesc(Long userId);

    List<Calendar> findByUserIdAndTypeOrderByStartDateDesc(Long userId, CalendarType type);

    List<Calendar> findByUserIdAndStartDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    List<Calendar> findByUserIdAndStartDateBetweenAndType(Long userId, LocalDateTime startDate, LocalDateTime endDate, CalendarType type);

    Optional<Calendar> findByEventId(Long eventId);

    List<Calendar> findByUserIdAndReminderTrue(Long userId);
}
