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
public class LlmAnalysisDto {
    private String status;
    private String eventType;
    private Map<String, Object> parsedData;
    private Float confidence;
    private String clarificationQuestion;
    private String aiResponse;
}
