package com.lifeai.repository;

import com.lifeai.entity.ClarificationQaRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClarificationQaRoundRepository extends JpaRepository<ClarificationQaRound, Long> {
    List<ClarificationQaRound> findBySessionIdOrderByRoundNumberAsc(UUID sessionId);
}
