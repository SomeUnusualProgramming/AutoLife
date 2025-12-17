package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MoodSuggestsActivityRule implements RecommendationRule {

    @Override
    public boolean canApply(Event event) {
        if (event.getType() != EventType.MOOD) {
            return false;
        }
        
        String description = event.getDescription().toLowerCase();
        String[] negativeKeywords = {
            "smutk", "depresj", "zły", "zła", "złe", "przygnę", "pełn strachu", "lęk", 
            "obawy", "stres", "napięci", "zdenerwowa", "sfrustra"
        };
        
        for (String keyword : negativeKeywords) {
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
            .text("Radzisz sobie? Spróbuj aktywności fizycznej - nawet krótki spacer może poprawić Twój nastrój. Ruch jest naturalnym antidepresantem!")
            .type(RecommendationType.PHYSICAL_ACTIVITY)
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
