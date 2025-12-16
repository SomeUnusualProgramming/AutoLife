package com.lifeai.recommendation.engine;

import com.lifeai.entity.Event;
import com.lifeai.entity.Recommendation;
import com.lifeai.recommendation.rule.RecommendationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecommendationRuleEngine {

    private final List<RecommendationRule> rules;

    public List<Recommendation> generateRecommendations(Event event) {
        return rules.stream()
            .sorted((rule1, rule2) -> Integer.compare(rule2.getPriority(), rule1.getPriority()))
            .filter(rule -> rule.canApply(event))
            .map(rule -> rule.generate(event))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
