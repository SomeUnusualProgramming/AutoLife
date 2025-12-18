package com.lifeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAgentRequest {
    private String inputType;
    private String textInput;
    private String transcribedText;
    private Long userId;
    private Map<String, Object> historicalContext;
}
