package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import com.jaypharma.aiservice.service.preprocessing.NormalizationTextUtils;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NormalizedExactMatchStrategy implements MatchStrategy {

    private final NormalizationKnowledgeStore knowledgeStore;

    public NormalizedExactMatchStrategy(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public Optional<DeterministicMatchResult> match(
            NormalizedSourceItem source,
            List<TargetItemNormalizer.NormalizedTarget> targets
    ) {
        String normalizedSource = NormalizationTextUtils.normalizeKey(source.normalizedValue());

        for (TargetItemNormalizer.NormalizedTarget target : targets) {
            if (normalizedSource.equals(target.normalizedName())
                    || normalizedSource.equals(target.normalizedKey())
                    || normalizedSource.equals(NormalizationTextUtils.normalizeKey(target.original().name()))
                    || normalizedSource.equals(NormalizationTextUtils.normalizeKey(target.original().key()))) {
                return Optional.of(new DeterministicMatchResult(
                        source.originalValue(),
                        target.original().key(),
                        target.original().name(),
                        MatchConfidence.HIGH,
                        strategyName(),
                        knowledgeStore.getThresholds().normalizedExactScore(),
                        "",
                        List.of()
                ));
            }
        }
        return Optional.empty();
    }

    @Override
    public String strategyName() {
        return "NORMALIZED_EXACT";
    }
}
