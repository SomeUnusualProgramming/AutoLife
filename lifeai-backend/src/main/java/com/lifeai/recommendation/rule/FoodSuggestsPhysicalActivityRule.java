package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.EventType;
import com.lifeai.entity.Recommendation;
import com.lifeai.entity.RecommendationPriority;
import com.lifeai.entity.RecommendationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FoodSuggestsPhysicalActivityRule implements RecommendationRule {

    private static final String[] SWEETS_KEYWORDS = {
        "cukier", "słodycze", "czekolada", "lód", "ciasto", "ciastko", "karmel",
        "toffee", "guma do żucia", "słodki napój", "napój gazowany", "sok",
        "candy", "sweet", "chocolate", "cake", "dessert", "candy", "caramel"
    };

    @Override
    public boolean canApply(Event event) {
        if (event.getType() != EventType.FOOD) {
            return false;
        }

        String description = event.getDescription().toLowerCase();
        for (String keyword : SWEETS_KEYWORDS) {
            if (description.contains(keyword.toLowerCase())) {
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
            .text("Zauważyliśmy, że spożywasz słodycze. Może teraz dobry czas na aktywność fizyczną? " +
                  "Spróbuj spaceru, biegania lub ćwiczeń, aby zbilansować kaloryjność posiłku.")
            .type(RecommendationType.PHYSICAL_ACTIVITY)
            .priority(RecommendationPriority.MEDIUM)
            .isApplied(false)
            .build();

        return Optional.of(recommendation);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
