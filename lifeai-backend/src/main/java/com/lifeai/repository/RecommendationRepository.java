package com.lifeai.repository;

import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByPriorityDescCreatedAtDesc(Long userId);

    List<Recommendation> findByUserIdAndIsAppliedOrderByCreatedAtDesc(Long userId, Boolean isApplied);

    List<Recommendation> findByUserIdAndTypeOrderByPriorityDescCreatedAtDesc(Long userId, RecommendationType type);

    List<Recommendation> findByEventId(Long eventId);

    Optional<Recommendation> findByEventIdAndType(Long eventId, RecommendationType type);

    List<Recommendation> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    List<Recommendation> findByUserIdAndPriority(Long userId, RecommendationPriority priority);
}
