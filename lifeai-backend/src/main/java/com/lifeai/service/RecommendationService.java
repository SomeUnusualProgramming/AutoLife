package com.lifeai.service;

import com.lifeai.dto.RecommendationResponse;
import com.lifeai.entity.Event;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import com.lifeai.recommendation.engine.RecommendationRuleEngine;
import com.lifeai.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationRuleEngine ruleEngine;

    public List<RecommendationResponse> generateRecommendationsForEvent(Event event) {
        List<Recommendation> recommendations = ruleEngine.generateRecommendations(event);
        return recommendations.stream()
            .map(this::saveAndConvertToResponse)
            .collect(Collectors.toList());
    }

    private RecommendationResponse saveAndConvertToResponse(Recommendation recommendation) {
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);
        return toRecommendationResponse(savedRecommendation);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByUserId(Long userId) {
        return recommendationRepository.findByUserIdOrderByPriorityDescCreatedAtDesc(userId)
            .stream()
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getUnreadRecommendations(Long userId) {
        return recommendationRepository.findByUserIdAndIsAppliedOrderByCreatedAtDesc(userId, false)
            .stream()
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByUserIdAndType(Long userId, RecommendationType type) {
        return recommendationRepository.findByUserIdAndTypeOrderByPriorityDescCreatedAtDesc(userId, type)
            .stream()
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByEventId(Long eventId) {
        return recommendationRepository.findByEventId(eventId)
            .stream()
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByUserIdAndPriority(Long userId, RecommendationPriority priority) {
        return recommendationRepository.findByUserIdAndPriority(userId, priority)
            .stream()
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    public RecommendationResponse applyRecommendation(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + recommendationId));

        recommendation.setIsApplied(true);
        recommendation.setAppliedAt(LocalDateTime.now());

        Recommendation updatedRecommendation = recommendationRepository.save(recommendation);
        return toRecommendationResponse(updatedRecommendation);
    }

    public void deleteRecommendation(Long recommendationId) {
        if (!recommendationRepository.existsById(recommendationId)) {
            throw new IllegalArgumentException("Recommendation not found with id: " + recommendationId);
        }
        recommendationRepository.deleteById(recommendationId);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendationById(Long id) {
        Recommendation recommendation = recommendationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + id));
        return toRecommendationResponse(recommendation);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getAllRecommendations() {
        return recommendationRepository.findAll().stream()
            .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
            .map(this::toRecommendationResponse)
            .collect(Collectors.toList());
    }

    public RecommendationResponse markRecommendationDone(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + recommendationId));

        recommendation.setIsApplied(true);
        recommendation.setAppliedAt(LocalDateTime.now());

        Recommendation updatedRecommendation = recommendationRepository.save(recommendation);
        return toRecommendationResponse(updatedRecommendation);
    }

    public RecommendationResponse markRecommendationPlanned(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + recommendationId));

        recommendation.setIsApplied(false);

        Recommendation updatedRecommendation = recommendationRepository.save(recommendation);
        return toRecommendationResponse(updatedRecommendation);
    }

    private RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        return RecommendationResponse.builder()
            .id(recommendation.getId())
            .eventId(recommendation.getEventId())
            .userId(recommendation.getUserId())
            .text(recommendation.getText())
            .type(recommendation.getType())
            .priority(recommendation.getPriority())
            .status(recommendation.getIsApplied() ? "DONE" : "PLANNED")
            .isApplied(recommendation.getIsApplied())
            .appliedAt(recommendation.getAppliedAt())
            .createdAt(recommendation.getCreatedAt())
            .updatedAt(recommendation.getUpdatedAt())
            .build();
    }
}
