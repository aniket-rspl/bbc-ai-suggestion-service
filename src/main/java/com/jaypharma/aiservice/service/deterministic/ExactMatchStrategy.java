package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ExactMatchStrategy implements MatchStrategy {

    private final NormalizationKnowledgeStore knowledgeStore;

    public ExactMatchStrategy(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public Optional<DeterministicMatchResult> match(
            NormalizedSourceItem source,
            List<TargetItemNormalizer.NormalizedTarget> targets
    ) {
        String original = source.originalValue().trim().toLowerCase(Locale.ROOT);
        String cleaned = source.cleanedValue();

        for (TargetItemNormalizer.NormalizedTarget target : targets) {
            if (equalsIgnoreCase(original, target.original().key())
                    || equalsIgnoreCase(original, target.original().name())
                    || equalsIgnoreCase(cleaned, target.original().key())
                    || equalsIgnoreCase(cleaned, target.original().name())) {
                return Optional.of(buildResult(source, target, knowledgeStore.getThresholds().exactMatchScore()));
            }
        }
        return Optional.empty();
    }

    @Override
    public String strategyName() {
        return "EXACT";
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private DeterministicMatchResult buildResult(
            NormalizedSourceItem source,
            TargetItemNormalizer.NormalizedTarget target,
            double score
    ) {
        return new DeterministicMatchResult(
                source.originalValue(),
                target.original().key(),
                target.original().name(),
                MatchConfidence.HIGH,
                strategyName(),
                score,
                "",
                List.of()
        );
    }
}
