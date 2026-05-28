package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import com.jaypharma.aiservice.service.preprocessing.NormalizationTextUtils;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SynonymMatchStrategy implements MatchStrategy {

    private final NormalizationKnowledgeStore knowledgeStore;

    public SynonymMatchStrategy(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public Optional<DeterministicMatchResult> match(
            NormalizedSourceItem source,
            List<TargetItemNormalizer.NormalizedTarget> targets
    ) {
        String normalizedSource = NormalizationTextUtils.normalizeKey(source.normalizedValue());
        Optional<String> canonical = knowledgeStore.resolveCanonicalPhrase(normalizedSource);
        if (canonical.isEmpty()) {
            return Optional.empty();
        }

        String canonicalPhrase = canonical.get();
        List<TargetItemNormalizer.NormalizedTarget> matches = new ArrayList<>();

        for (TargetItemNormalizer.NormalizedTarget target : targets) {
            if (canonicalPhrase.equals(target.normalizedName())
                    || canonicalPhrase.equals(target.normalizedKey())
                    || target.normalizedName().contains(canonicalPhrase)
                    || canonicalPhrase.contains(target.normalizedName())) {
                matches.add(target);
            }
        }

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        TargetItemNormalizer.NormalizedTarget best = matches.get(0);
        List<String> alternatives = matches.stream()
                .skip(1)
                .map(target -> target.original().key())
                .toList();

        return Optional.of(new DeterministicMatchResult(
                source.originalValue(),
                best.original().key(),
                best.original().name(),
                MatchConfidence.HIGH,
                strategyName(),
                knowledgeStore.getThresholds().synonymScore(),
                canonicalPhrase,
                alternatives
        ));
    }

    @Override
    public String strategyName() {
        return "SYNONYM";
    }
}
