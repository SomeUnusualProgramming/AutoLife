package com.lifeai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_user_id", columnList = "user_id"),
    @Index(name = "idx_events_event_type", columnList = "event_type"),
    @Index(name = "idx_events_event_timestamp", columnList = "event_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType type;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Timestamp is required")
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
