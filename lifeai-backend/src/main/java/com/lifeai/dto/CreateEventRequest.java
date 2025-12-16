package com.lifeai.dto;

import com.lifeai.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateEventRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Event type is required")
    private EventType type;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
