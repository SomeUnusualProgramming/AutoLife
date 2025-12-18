package com.lifeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAgentResponse {
    private String status;
    private String action;
    private EventResponse event;
    private PendingEventDto pendingEvent;
    private String clarificationQuestion;
    private Integer clarificationRound;
    private UUID sessionId;
    private String aiResponse;
    private Float confidence;
    private List<String> warningFlags;
}
