package com.jaypharma.aiservice.service.deterministic;

import java.util.List;

public record DeterministicMatchResult(
        String sourceItem,
        String targetKey,
        String targetName,
        MatchConfidence confidence,
        String strategyName,
        double score,
        String reason,
        List<String> alternatives
) {
}
