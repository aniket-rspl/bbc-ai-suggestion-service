package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AmbiguityDetector {

    private final NormalizationKnowledgeStore knowledgeStore;

    public AmbiguityDetector(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    public boolean isPreprocessorAmbiguous(NormalizedSourceItem source) {
        return source.ambiguous();
    }

    public MatchConfidence applyScoreAmbiguity(
            NormalizedSourceItem source,
            List<DeterministicMatchResult> rankedResults
    ) {
        if (source.ambiguous()) {
            return MatchConfidence.NONE;
        }
        if (rankedResults.isEmpty()) {
            return MatchConfidence.NONE;
        }

        List<DeterministicMatchResult> sorted = rankedResults.stream()
                .sorted(Comparator.comparingDouble(DeterministicMatchResult::score).reversed())
                .toList();

        DeterministicMatchResult top = sorted.get(0);
        MatchConfidence baseConfidence = resolveConfidenceFromScore(top);

        if (sorted.size() > 1) {
            boolean allSameTarget = sorted.stream()
                    .allMatch(result -> top.targetKey().equals(result.targetKey()));
            if (allSameTarget) {
                return MatchConfidence.HIGH;
            }
            DeterministicMatchResult second = sorted.get(1);
            double gap = top.score() - second.score();
            if (gap < knowledgeStore.getThresholds().ambiguityGap()) {
                return moreConservative(baseConfidence, MatchConfidence.MEDIUM);
            }
        }
        return moreConservative(baseConfidence, top.confidence());
    }

    public boolean isResolvableHigh(MatchConfidence confidence) {
        return confidence == MatchConfidence.HIGH;
    }

    private MatchConfidence resolveConfidenceFromScore(DeterministicMatchResult result) {
        NormalizationKnowledgeStore.NormalizationThresholds thresholds = knowledgeStore.getThresholds();
        if (result.score() >= thresholds.high()) {
            return MatchConfidence.HIGH;
        }
        if (result.score() >= thresholds.medium()) {
            return MatchConfidence.MEDIUM;
        }
        return MatchConfidence.LOW;
    }

    private MatchConfidence moreConservative(MatchConfidence first, MatchConfidence second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
