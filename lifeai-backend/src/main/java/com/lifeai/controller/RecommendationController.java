package com.lifeai.controller;

import com.lifeai.dto.RecommendationResponse;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import com.lifeai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByUserId(@PathVariable Long userId) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsByUserId(userId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<List<RecommendationResponse>> getUnreadRecommendations(@PathVariable Long userId) {
        List<RecommendationResponse> recommendations = recommendationService.getUnreadRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/users/{userId}/type/{type}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByType(
            @PathVariable Long userId,
            @PathVariable RecommendationType type) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsByUserIdAndType(userId, type);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/users/{userId}/priority/{priority}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByPriority(
            @PathVariable Long userId,
            @PathVariable RecommendationPriority priority) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsByUserIdAndPriority(userId, priority);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByEventId(@PathVariable Long eventId) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsByEventId(eventId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> getRecommendationById(@PathVariable Long id) {
        RecommendationResponse recommendation = recommendationService.getRecommendationById(id);
        return ResponseEntity.ok(recommendation);
    }

    @PatchMapping("/{id}/apply")
    public ResponseEntity<RecommendationResponse> applyRecommendation(@PathVariable Long id) {
        RecommendationResponse recommendation = recommendationService.applyRecommendation(id);
        return ResponseEntity.ok(recommendation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationService.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }
}
