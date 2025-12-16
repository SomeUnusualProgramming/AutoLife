package com.lifeai.ai;

import com.lifeai.ai.dto.AiAnalysisRequest;
import com.lifeai.ai.dto.AiAnalysisResponse;

public interface LifeAiService {

    AiAnalysisResponse analyzeHealthData(AiAnalysisRequest request);

    String generateRecommendation(String context, String topic);

    boolean isAvailable();
}
