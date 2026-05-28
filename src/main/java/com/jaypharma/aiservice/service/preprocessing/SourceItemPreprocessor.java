package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.dto.model.NormalizedSourceItem;
import com.jaypharma.aiservice.dto.module.DetectedFieldType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SourceItemPreprocessor {

    private final NormalizationKnowledgeStore knowledgeStore;
    private final FieldTypeHeuristicService fieldTypeHeuristicService;

    public SourceItemPreprocessor(
            NormalizationKnowledgeStore knowledgeStore,
            FieldTypeHeuristicService fieldTypeHeuristicService
    ) {
        this.knowledgeStore = knowledgeStore;
        this.fieldTypeHeuristicService = fieldTypeHeuristicService;
    }

    public NormalizedSourceItem preprocess(String sourceItem) {
        String original = sourceItem == null ? "" : sourceItem.trim();
        String cleaned = cleanAndTokenExpand(original);
        String normalized = toCanonicalPhrase(cleaned);
        List<String> ambiguityReasons = detectAmbiguity(original, cleaned);
        boolean ambiguous = !ambiguityReasons.isEmpty();
        DetectedFieldType fieldType = fieldTypeHeuristicService.detect(normalized);

        return new NormalizedSourceItem(
                original,
                cleaned,
                normalized,
                fieldType,
                ambiguous,
                List.copyOf(ambiguityReasons)
        );
    }

    private String cleanAndTokenExpand(String value) {
        String working = splitCamelCase(value);
        working = preserveHashToken(working);
        working = basicCleanup(working);
        working = removeNoiseTokens(working);
        String phraseKey = NormalizationTextUtils.normalizeKey(working);
        if (knowledgeStore.resolveCanonicalPhrase(phraseKey).isPresent()) {
            return phraseKey;
        }
        return expandCleanedPhaseTokens(working);
    }

    private String preserveHashToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("#", " number");
    }

    private String toCanonicalPhrase(String cleanedPhrase) {
        String key = NormalizationTextUtils.normalizeKey(cleanedPhrase);
        Optional<String> canonical = knowledgeStore.resolveCanonicalPhrase(key);
        if (canonical.isPresent()) {
            return canonical.get();
        }
        return expandTokensToCanonical(cleanedPhrase);
    }

    private String expandTokensToCanonical(String phrase) {
        List<String> tokens = tokenize(phrase);
        List<String> expanded = new ArrayList<>();
        for (String token : tokens) {
            Optional<String> canonicalToken = knowledgeStore.resolveAliasToken(token);
            if (canonicalToken.isPresent()) {
                expanded.addAll(tokenize(canonicalToken.get()));
            } else {
                expanded.add(token);
            }
        }
        return joinTokens(expanded);
    }

    private String expandCleanedPhaseTokens(String phrase) {
        Set<String> cleanedPhaseTokens = Set.copyOf(knowledgeStore.getCleanedPhaseTokens());
        List<String> tokens = tokenize(phrase);
        List<String> expanded = new ArrayList<>();
        for (String token : tokens) {
            if (shouldExpandInCleanedPhase(token, cleanedPhaseTokens)) {
                Optional<String> canonicalToken = knowledgeStore.resolveAliasToken(token);
                if (canonicalToken.isPresent()) {
                    expanded.addAll(tokenize(canonicalToken.get()));
                } else {
                    expanded.add(token);
                }
            } else {
                expanded.add(token);
            }
        }
        return joinTokens(expanded);
    }

    private boolean shouldExpandInCleanedPhase(String token, Set<String> cleanedPhaseTokens) {
        return cleanedPhaseTokens.contains(token);
    }

    private List<String> detectAmbiguity(String original, String cleaned) {
        List<String> reasons = new ArrayList<>();
        String originalKey = NormalizationTextUtils.normalizeKey(original);
        String cleanedKey = NormalizationTextUtils.normalizeKey(cleaned);

        for (String vague : knowledgeStore.getVagueSourcePatterns()) {
            if (originalKey.equals(vague) || cleanedKey.equals(vague)) {
                reasons.add(knowledgeStore.getMessages().ambiguousSourceReason());
                break;
            }
        }

        for (NormalizationKnowledgeStore.AmbiguousEntry entry : knowledgeStore.getAmbiguousEntries()) {
            if (entry.aliases().contains(originalKey) || entry.aliases().contains(cleanedKey)) {
                if (entry.ambiguityReason() != null && !entry.ambiguityReason().isBlank()) {
                    reasons.add(entry.ambiguityReason());
                } else {
                    reasons.add(knowledgeStore.getMessages().ambiguousSourceReason());
                }
            }
        }

        return reasons.stream().distinct().toList();
    }

    private String basicCleanup(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String result = value.trim().toLowerCase(Locale.ROOT);
        result = result.replaceAll("[\\/_\\-.]+", " ");
        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }

    private String splitCamelCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String withSpaces = value.replaceAll("([a-z])([A-Z])", "$1 $2");
        return withSpaces.toLowerCase(Locale.ROOT);
    }

    private String removeNoiseTokens(String phrase) {
        List<String> tokens = tokenize(phrase);
        List<String> noise = knowledgeStore.getNoiseTokens();
        return tokens.stream()
                .filter(token -> noise.stream().noneMatch(noiseToken -> noiseToken.equalsIgnoreCase(token)))
                .collect(Collectors.joining(" "));
    }

    private String joinTokens(List<String> tokens) {
        return String.join(" ", tokens).trim().replaceAll("\\s+", " ");
    }

    private List<String> tokenize(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return List.of();
        }
        return Arrays.stream(phrase.trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }
}
