package com.lifeai.recommendation.rule;

import com.lifeai.entity.Event;
import com.lifeai.entity.Recommendation;

import java.util.Optional;

public interface RecommendationRule {

    boolean canApply(Event event);

    Optional<Recommendation> generate(Event event);

    int getPriority();
}
