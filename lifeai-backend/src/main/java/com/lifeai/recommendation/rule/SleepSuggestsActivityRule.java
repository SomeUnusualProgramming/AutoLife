package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SleepSuggestsActivityRule implements RecommendationRule {

    @Override
    public boolean canApply(Event event) {
        if (event.getType() != EventType.SLEEP) {
            return false;
        }
        
        String description = event.getDescription().toLowerCase();
        String[] insufficientSleep = {
            "mało", "brak", "krótk", "niewyspan", "źle", "trudno", "bezsenną", "godzin"
        };
        
        for (String keyword : insufficientSleep) {
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
            .text("Nie dostajesz wystarczająco dużo snu. Staraj się iść spać wcześniej i utrzymywać regularny harmonogram. Dobry sen to podstawa zdrowia!")
            .type(RecommendationType.SLEEP_IMPROVEMENT)
            .priority(RecommendationPriority.HIGH)
            .isApplied(false)
            .build();

        return Optional.of(recommendation);
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
