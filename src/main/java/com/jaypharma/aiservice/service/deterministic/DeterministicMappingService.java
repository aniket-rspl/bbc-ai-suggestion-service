package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.dto.model.SuggestionItemDto;
import com.jaypharma.aiservice.dto.model.TargetItemDto;
import com.jaypharma.aiservice.service.preprocessing.SourceItemPreprocessor;
import com.jaypharma.aiservice.service.preprocessing.TargetItemNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DeterministicMappingService {

    private final SourceItemPreprocessor sourceItemPreprocessor;
    private final TargetItemNormalizer targetItemNormalizer;
    private final List<MatchStrategy> strategies;
    private final AmbiguityDetector ambiguityDetector;
    private final DeterministicSuggestionMapper suggestionMapper;

    public DeterministicMappingService(
            SourceItemPreprocessor sourceItemPreprocessor,
            TargetItemNormalizer targetItemNormalizer,
            ExactMatchStrategy exactMatchStrategy,
            NormalizedExactMatchStrategy normalizedExactMatchStrategy,
            SynonymMatchStrategy synonymMatchStrategy,
            FuzzyMatchStrategy fuzzyMatchStrategy,
            AmbiguityDetector ambiguityDetector,
            DeterministicSuggestionMapper suggestionMapper
    ) {
        this.sourceItemPreprocessor = sourceItemPreprocessor;
        this.targetItemNormalizer = targetItemNormalizer;
        this.strategies = List.of(
                exactMatchStrategy,
                normalizedExactMatchStrategy,
                synonymMatchStrategy,
                fuzzyMatchStrategy
        );
        this.ambiguityDetector = ambiguityDetector;
        this.suggestionMapper = suggestionMapper;
    }

    public Optional<SuggestionItemDto> matchSourceItem(
            String sourceItem,
            List<TargetItemDto> targetItems
    ) {
        NormalizedSourceItem normalizedSource = sourceItemPreprocessor.preprocess(sourceItem);
        if (ambiguityDetector.isPreprocessorAmbiguous(normalizedSource)) {
            return Optional.empty();
        }

        List<TargetItemNormalizer.NormalizedTarget> normalizedTargets = targetItems.stream()
                .map(targetItemNormalizer::normalize)
                .toList();

        List<DeterministicMatchResult> candidates = new ArrayList<>();
        for (MatchStrategy strategy : strategies) {
            strategy.match(normalizedSource, normalizedTargets)
                    .ifPresent(candidates::add);
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        List<DeterministicMatchResult> ranked = candidates.stream()
                .sorted(Comparator.comparingDouble(DeterministicMatchResult::score).reversed())
                .toList();

        MatchConfidence finalConfidence = ambiguityDetector.applyScoreAmbiguity(normalizedSource, ranked);
        if (!ambiguityDetector.isResolvableHigh(finalConfidence)) {
            return Optional.empty();
        }

        DeterministicMatchResult best = ranked.get(0);
        DeterministicMatchResult withReason = new DeterministicMatchResult(
                best.sourceItem(),
                best.targetKey(),
                best.targetName(),
                MatchConfidence.HIGH,
                best.strategyName(),
                best.score(),
                best.reason(),
                best.alternatives()
        );
        return Optional.of(suggestionMapper.toSuggestionItem(withReason));
    }

    public NormalizedSourceItem preprocess(String sourceItem) {
        return sourceItemPreprocessor.preprocess(sourceItem);
    }
}
