package com.lifeai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long id;

    private Long eventId;

    private Long userId;

    @JsonProperty("suggestion")
    private String text;

    @JsonProperty("category")
    private RecommendationType type;

    private RecommendationPriority priority;

    @JsonProperty("status")
    private String status;

    @JsonProperty("actionUrl")
    private String actionUrl;

    @JsonProperty("aiGenerated")
    private Boolean aiGenerated;

    private Boolean isApplied;

    private LocalDateTime appliedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
