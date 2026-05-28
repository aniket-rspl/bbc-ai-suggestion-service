package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;

import java.util.List;
import java.util.Optional;

public interface MatchStrategy {

    Optional<DeterministicMatchResult> match(
            NormalizedSourceItem source,
            List<TargetItemNormalizer.NormalizedTarget> targets
    );

    String strategyName();
}
