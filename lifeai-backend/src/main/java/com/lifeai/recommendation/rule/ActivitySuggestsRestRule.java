package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ActivitySuggestsRestRule implements RecommendationRule {

    @Override
    public boolean canApply(Event event) {
        if (event.getType() != EventType.ACTIVITY) {
            return false;
        }
        
        String description = event.getDescription().toLowerCase();
        String[] intensiveKeywords = {
            "bieg", "trening", "ćwicz", "siłownia", "maraton", "sprint", 
            "intensywn", "ciężk", "wysyłk"
        };
        
        for (String keyword : intensiveKeywords) {
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
            .text("Dobrze że byłeś aktywny! Zadbaj teraz o regenerację - wystarczy 15-30 minut odpoczynku i nawodnienia. To ważne dla twojego zdrowia.")
            .type(RecommendationType.SLEEP_IMPROVEMENT)
            .priority(RecommendationPriority.MEDIUM)
            .isApplied(false)
            .build();

        return Optional.of(recommendation);
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
