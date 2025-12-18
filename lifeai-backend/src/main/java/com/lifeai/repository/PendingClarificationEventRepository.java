package com.lifeai.repository;

import com.lifeai.entity.PendingClarificationEvent;
import com.lifeai.entity.enums.ClarificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface PendingClarificationEventRepository extends JpaRepository<PendingClarificationEvent, Long> {
    Optional<PendingClarificationEvent> findBySessionId(UUID sessionId);

    List<PendingClarificationEvent> findByUserIdAndStatus(Long userId, ClarificationStatus status);

    Optional<PendingClarificationEvent> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ClarificationStatus status);
}
