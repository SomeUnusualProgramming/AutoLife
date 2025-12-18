package com.lifeai.repository;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventStatus;
import com.lifeai.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserId(Long userId);
    
    List<Event> findByUserIdAndType(Long userId, EventType type);
    
    List<Event> findByUserIdAndTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT e FROM Event e WHERE e.userId = :userId ORDER BY e.timestamp DESC")
    List<Event> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
    
    @Query("SELECT e FROM Event e WHERE e.userId = :userId AND e.type = :type ORDER BY e.timestamp DESC")
    List<Event> findByUserIdAndTypeOrderByTimestampDesc(@Param("userId") Long userId, @Param("type") EventType type);

    @Query("SELECT e FROM Event e WHERE e.userId = :userId AND e.status = :status ORDER BY e.timestamp DESC")
    List<Event> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.status = :status")
    Optional<Event> findByIdAndStatus(@Param("id") Long id, @Param("status") EventStatus status);
}
