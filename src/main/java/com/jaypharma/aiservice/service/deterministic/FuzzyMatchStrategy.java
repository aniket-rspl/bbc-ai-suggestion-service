package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class FuzzyMatchStrategy implements MatchStrategy {

    private final NormalizationKnowledgeStore knowledgeStore;

    public FuzzyMatchStrategy(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public Optional<DeterministicMatchResult> match(
            NormalizedSourceItem source,
            List<TargetItemNormalizer.NormalizedTarget> targets
    ) {
        String sourcePhrase = source.normalizedValue();
        if (sourcePhrase == null || sourcePhrase.isBlank()) {
            return Optional.empty();
        }

        NormalizationKnowledgeStore.NormalizationThresholds thresholds = knowledgeStore.getThresholds();
        List<ScoredTarget> scoredTargets = new ArrayList<>();

        for (TargetItemNormalizer.NormalizedTarget target : targets) {
            double nameScore = TextSimilarityUtils.similarity(sourcePhrase, target.normalizedName());
            double keyScore = TextSimilarityUtils.similarity(sourcePhrase, target.normalizedKey());
            double bestScore = Math.max(nameScore, keyScore);

            if (bestScore >= thresholds.fuzzyMinCandidateScore()) {
                scoredTargets.add(new ScoredTarget(target, bestScore));
            }
        }

        if (scoredTargets.isEmpty()) {
            return Optional.empty();
        }

        scoredTargets.sort(Comparator.comparingDouble(ScoredTarget::score).reversed());
        ScoredTarget best = scoredTargets.get(0);
        MatchConfidence confidence = resolveConfidence(best.score(), thresholds);

        if (confidence == MatchConfidence.NONE || confidence == MatchConfidence.LOW) {
            return Optional.empty();
        }

        List<String> alternatives = scoredTargets.stream()
                .skip(1)
                .filter(candidate -> !candidate.target().original().key().equals(best.target().original().key()))
                .filter(candidate -> best.score() - candidate.score() <= thresholds.ambiguityGap())
                .map(candidate -> candidate.target().original().key())
                .toList();

        String detail = String.format(
                Locale.ROOT,
                "%s (similarity %.2f)",
                best.target().original().name(),
                best.score()
        );

        return Optional.of(new DeterministicMatchResult(
                source.originalValue(),
                best.target().original().key(),
                best.target().original().name(),
                confidence,
                strategyName(),
                best.score(),
                detail,
                alternatives
        ));
    }

    private MatchConfidence resolveConfidence(
            double score,
            NormalizationKnowledgeStore.NormalizationThresholds thresholds
    ) {
        if (score >= thresholds.high()) {
            return MatchConfidence.HIGH;
        }
        if (score >= thresholds.medium()) {
            return MatchConfidence.MEDIUM;
        }
        return MatchConfidence.NONE;
    }

    @Override
    public String strategyName() {
        return "FUZZY";
    }

    private record ScoredTarget(TargetItemNormalizer.NormalizedTarget target, double score) {
    }
}
