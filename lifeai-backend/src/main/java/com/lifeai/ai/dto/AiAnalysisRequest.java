package com.lifeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {

    private Long userId;

    private String dataType;

    private String content;

    private Map<String, Object> metadata;

    private String analysisMode;
}
