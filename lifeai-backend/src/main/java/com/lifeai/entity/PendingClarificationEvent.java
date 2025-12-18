package com.lifeai.entity;

import com.lifeai.entity.enums.ClarificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pending_clarification_events", indexes = {
    @Index(name = "idx_pending_events_user_id", columnList = "user_id"),
    @Index(name = "idx_pending_events_session_id", columnList = "session_id"),
    @Index(name = "idx_pending_events_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingClarificationEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", columnDefinition = "UUID", nullable = false, unique = true)
    private UUID sessionId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawInput;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parsedData;

    private Float confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClarificationStatus status = ClarificationStatus.PENDING_CLARIFICATION;

    @Column(nullable = false)
    private Integer clarificationRound = 0;
}
