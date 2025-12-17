package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SymptomSuggestsDoctorVisitRule implements RecommendationRule {

    @Override
    public boolean canApply(Event event) {
        if (event.getType() != EventType.SYMPTOM) {
            return false;
        }
        
        String description = event.getDescription().toLowerCase();
        String[] severeKeywords = {
            "poważn", "ostry", "intensywn", "ciągł", "nie ustępuj", "pogorszył",
            "gorączk", "wysoka temp", "duszno", "ból w klatc"
        };
        
        for (String keyword : severeKeywords) {
            if (description.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Recommendation> generate(Event event) {
        if (!canApply(event)) {
            return Optional.empty();
        }

        Recommendation recommendation = Recommendation.builder()
            .eventId(event.getId())
            .userId(event.getUserId())
            .text("Masz poważne objawy. Zalecam aby skontaktować się z lekarzem możliwie jak najszybciej. Twoje zdrowie jest ważne!")
            .type(RecommendationType.DOCTOR_VISIT)
            .priority(RecommendationPriority.HIGH)
            .isApplied(false)
            .build();

        return Optional.of(recommendation);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
