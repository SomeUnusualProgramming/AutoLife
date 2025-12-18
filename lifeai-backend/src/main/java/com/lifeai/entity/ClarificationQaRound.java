package com.lifeai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "clarification_qa_rounds", indexes = {
    @Index(name = "idx_clarification_qa_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClarificationQaRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, columnDefinition = "UUID")
    private ClarificationSession session;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(columnDefinition = "TEXT")
    private String aiQuestion;

    @Column(columnDefinition = "TEXT")
    private String userResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> aiAnalysis;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
