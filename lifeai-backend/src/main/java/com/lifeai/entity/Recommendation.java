package com.lifeai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations", indexes = {
    @Index(name = "idx_rec_event_id", columnList = "event_id"),
    @Index(name = "idx_rec_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation extends BaseEntity {

    @NotNull(message = "Event ID is required")
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "Recommendation text is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @NotNull(message = "Recommendation type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationPriority priority;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isApplied = false;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
}
