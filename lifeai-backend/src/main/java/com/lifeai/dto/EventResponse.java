package com.lifeai.dto;

import com.lifeai.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private Long userId;
    private EventType type;
    private String description;
    private LocalDateTime timestamp;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
