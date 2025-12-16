package com.lifeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    private String analysisId;

    private Long userId;

    private String status;

    private String summary;

    private List<AiHealthRecommendation> recommendations;

    private Map<String, Object> insights;

    private Map<String, Object> rawData;

    private LocalDateTime analyzedAt;

    private Double confidence;
}
