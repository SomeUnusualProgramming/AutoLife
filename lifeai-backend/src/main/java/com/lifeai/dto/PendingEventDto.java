package com.lifeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingEventDto {
    private Long id;
    private UUID sessionId;
    private String type;
    private String rawInput;
    private Map<String, Object> metadata;
    private Float confidence;
    private String status;
    private Integer clarificationRound;
}
