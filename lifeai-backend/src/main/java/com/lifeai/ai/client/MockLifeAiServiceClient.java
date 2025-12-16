package com.lifeai.ai.client;

import com.lifeai.ai.LifeAiService;
import com.lifeai.ai.dto.AiAnalysisRequest;
import com.lifeai.ai.dto.AiAnalysisResponse;
import com.lifeai.ai.dto.AiHealthRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.service.enabled", havingValue = "false", matchIfMissing = true)
public class MockLifeAiServiceClient implements LifeAiService {

    @Override
    public AiAnalysisResponse analyzeHealthData(AiAnalysisRequest request) {
        log.info("Mock: Analyzing health data for user {}", request.getUserId());

        return AiAnalysisResponse.builder()
                .analysisId("MOCK-" + System.currentTimeMillis())
                .userId(request.getUserId())
                .status("completed")
                .summary("Mock analysis summary for " + request.getDataType())
                .recommendations(List.of(
                        AiHealthRecommendation.builder()
                                .id("rec-1")
                                .type("physical_activity")
                                .title("Increase Physical Activity")
                                .description("Based on your current data, try to increase daily physical activity")
                                .priority("medium")
                                .score(65)
                                .actionable("true")
                                .build(),
                        AiHealthRecommendation.builder()
                                .id("rec-2")
                                .type("nutrition")
                                .title("Improve Nutrition")
                                .description("Consider adding more vegetables to your diet")
                                .priority("medium")
                                .score(72)
                                .actionable("true")
                                .build()
                ))
                .insights(Map.of(
                        "overall_health_score", 75,
                        "trend", "improving"
                ))
                .analyzedAt(LocalDateTime.now())
                .confidence(0.85)
                .build();
    }

    @Override
    public String generateRecommendation(String context, String topic) {
        log.info("Mock: Generating recommendation for topic: {}", topic);
        return "This is a mock recommendation for " + topic + " based on context: " + context;
    }

    @Override
    public boolean isAvailable() {
        log.info("Mock AI Service is always available");
        return true;
    }
}
