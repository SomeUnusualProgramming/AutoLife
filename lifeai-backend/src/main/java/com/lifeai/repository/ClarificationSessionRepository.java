package com.lifeai.repository;

import com.lifeai.entity.ClarificationSession;
import com.lifeai.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClarificationSessionRepository extends JpaRepository<ClarificationSession, UUID> {
    List<ClarificationSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    List<ClarificationSession> findByUserId(Long userId);
}
