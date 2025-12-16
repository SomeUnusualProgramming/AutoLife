package com.lifeai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHealthRecommendation {

    private String id;

    private String type;

    private String title;

    private String description;

    private String priority;

    private Integer score;

    private String actionable;
}
