package com.lifeai.controller;

import com.lifeai.ai.LifeAiService;
import com.lifeai.ai.dto.AiAnalysisRequest;
import com.lifeai.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final LifeAiService aiService;

    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzeHealthData(
            @RequestBody AiAnalysisRequest request) {
        AiAnalysisResponse response = aiService.analyzeHealthData(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkAiServiceHealth() {
        boolean available = aiService.isAvailable();
        return ResponseEntity.ok(available);
    }
}
