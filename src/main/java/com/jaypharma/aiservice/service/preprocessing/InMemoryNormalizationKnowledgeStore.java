package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.config.NormalizationProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryNormalizationKnowledgeStore implements NormalizationKnowledgeStore {

    private final NormalizationThresholds thresholds;
    private final NormalizationMessages messages;
    private final List<String> cleanedPhaseTokens;
    private final List<String> noiseTokens;
    private final List<String> vagueSourcePatterns;
    private final List<AmbiguousEntry> ambiguousEntries;
    private final Map<String, String> fieldTypeHints;
    private final Map<String, Set<String>> keywordGroups;
    private final Map<String, String> aliasToCanonical;
    private final Map<String, String> overlayAliasToCanonical;

    public InMemoryNormalizationKnowledgeStore(NormalizationProperties properties) {
        this.thresholds = new NormalizationThresholds(
                properties.getThresholds().getHigh(),
                properties.getThresholds().getMedium(),
                properties.getThresholds().getAmbiguityGap(),
                properties.getThresholds().getExactMatchScore(),
                properties.getThresholds().getNormalizedExactScore(),
                properties.getThresholds().getSynonymScore(),
                properties.getThresholds().getFuzzyMinCandidateScore()
        );
        this.messages = new NormalizationMessages(
                properties.getMessages().getDeterministicHighReason(),
                properties.getMessages().getDeterministicMediumReason(),
                properties.getMessages().getManualReviewWarning(),
                properties.getMessages().getNoSuggestionReason(),
                properties.getMessages().getAmbiguousSourceReason()
        );
        this.cleanedPhaseTokens = List.copyOf(properties.getCleanedPhaseTokens());
        this.noiseTokens = List.copyOf(properties.getNoiseTokens());
        this.vagueSourcePatterns = List.copyOf(properties.getVagueSourcePatterns());
        this.ambiguousEntries = buildAmbiguousEntries(properties);
        this.fieldTypeHints = buildFieldTypeHints(properties);
        this.keywordGroups = new ConcurrentHashMap<>(buildKeywordGroups(properties));
        this.aliasToCanonical = new ConcurrentHashMap<>(buildAliasIndex(properties));
        this.overlayAliasToCanonical = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<String> resolveCanonicalPhrase(String normalizedPhrase) {
        if (normalizedPhrase == null || normalizedPhrase.isBlank()) {
            return Optional.empty();
        }
        String key = normalizeKey(normalizedPhrase);
        String overlay = overlayAliasToCanonical.get(key);
        if (overlay != null) {
            return Optional.of(overlay);
        }
        return Optional.ofNullable(aliasToCanonical.get(key));
    }

    @Override
    public Optional<String> resolveAliasToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = normalizeKey(token);
        String overlay = overlayAliasToCanonical.get(key);
        if (overlay != null) {
            return Optional.of(overlay);
        }
        String canonical = aliasToCanonical.get(key);
        if (canonical != null) {
            return Optional.of(canonical);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Set<String>> getKeywordGroups() {
        Map<String, Set<String>> merged = new LinkedHashMap<>();
        keywordGroups.forEach((canonical, aliases) ->
                merged.put(canonical, Set.copyOf(aliases))
        );
        return Collections.unmodifiableMap(merged);
    }

    @Override
    public List<AmbiguousEntry> getAmbiguousEntries() {
        return List.copyOf(ambiguousEntries);
    }

    @Override
    public List<String> getCleanedPhaseTokens() {
        return cleanedPhaseTokens;
    }

    @Override
    public List<String> getNoiseTokens() {
        return noiseTokens;
    }

    @Override
    public List<String> getVagueSourcePatterns() {
        return vagueSourcePatterns;
    }

    @Override
    public NormalizationThresholds getThresholds() {
        return thresholds;
    }

    @Override
    public NormalizationMessages getMessages() {
        return messages;
    }

    @Override
    public List<String> getFieldTypeHintTokens(String fieldType) {
        return fieldTypeHints.entrySet().stream()
                .filter(entry -> entry.getValue().equals(fieldType))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public void upsertAlias(String canonical, String alias, String source) {
        if (canonical == null || canonical.isBlank() || alias == null || alias.isBlank()) {
            return;
        }
        String canonicalPhrase = normalizeKey(canonical);
        String aliasKey = normalizeKey(alias);
        overlayAliasToCanonical.put(aliasKey, canonicalPhrase);
        keywordGroups.computeIfAbsent(canonicalPhrase, ignored -> ConcurrentHashMap.newKeySet())
                .add(aliasKey);
        log.info("Normalization alias upserted. canonical={}, alias={}, source={}", canonicalPhrase, aliasKey, source);
    }

    @Override
    public String upsertFromApprovedMapping(
            String sourceItem,
            String finalTargetKey,
            String finalTargetName
    ) {
        if (sourceItem == null || sourceItem.isBlank()) {
            return "";
        }
        String canonical = deriveCanonicalFromTarget(finalTargetKey, finalTargetName);
        if (canonical.isBlank()) {
            return "";
        }
        upsertAlias(canonical, sourceItem, "mapping-decision");
        return canonical;
    }

    private String deriveCanonicalFromTarget(String finalTargetKey, String finalTargetName) {
        if (finalTargetName != null && !finalTargetName.isBlank()) {
            return normalizeKey(finalTargetName);
        }
        if (finalTargetKey != null && !finalTargetKey.isBlank()) {
            return normalizeKey(finalTargetKey.replace('_', ' '));
        }
        return "";
    }

    private static Map<String, Set<String>> buildKeywordGroups(NormalizationProperties properties) {
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        for (NormalizationProperties.KeywordGroup group : properties.getKeywordGroups()) {
            if (group.getCanonical() == null || group.getCanonical().isBlank()) {
                continue;
            }
            String canonical = normalizeKey(group.getCanonical());
            Set<String> aliases = groups.computeIfAbsent(canonical, ignored -> ConcurrentHashMap.newKeySet());
            aliases.add(canonical);
            for (String alias : group.getAliases()) {
                if (alias != null && !alias.isBlank()) {
                    aliases.add(normalizeKey(alias));
                }
            }
        }
        return groups;
    }

    private static Map<String, String> buildAliasIndex(NormalizationProperties properties) {
        Map<String, String> index = new LinkedHashMap<>();
        for (NormalizationProperties.KeywordGroup group : properties.getKeywordGroups()) {
            if (group.getCanonical() == null || group.getCanonical().isBlank()) {
                continue;
            }
            String canonical = normalizeKey(group.getCanonical());
            index.put(canonical, canonical);
            for (String alias : group.getAliases()) {
                if (alias != null && !alias.isBlank()) {
                    index.put(normalizeKey(alias), canonical);
                }
            }
        }
        for (NormalizationProperties.AmbiguousKeywordGroup group : properties.getAmbiguousKeywordGroups()) {
            if (group.getCanonical() == null || group.getCanonical().isBlank()) {
                continue;
            }
            String canonical = normalizeKey(group.getCanonical());
            index.putIfAbsent(canonical, canonical);
            for (String alias : group.getAliases()) {
                if (alias != null && !alias.isBlank()) {
                    index.putIfAbsent(normalizeKey(alias), canonical);
                }
            }
        }
        return index;
    }

    private static List<AmbiguousEntry> buildAmbiguousEntries(NormalizationProperties properties) {
        return properties.getAmbiguousKeywordGroups().stream()
                .filter(group -> group.getCanonical() != null && !group.getCanonical().isBlank())
                .map(group -> new AmbiguousEntry(
                        normalizeKey(group.getCanonical()),
                        group.getAliases().stream()
                                .filter(alias -> alias != null && !alias.isBlank())
                                .map(InMemoryNormalizationKnowledgeStore::normalizeKey)
                                .collect(Collectors.toCollection(LinkedHashSet::new)),
                        group.getAmbiguityReason() == null ? "" : group.getAmbiguityReason()
                ))
                .toList();
    }

    private static Map<String, String> buildFieldTypeHints(NormalizationProperties properties) {
        Map<String, String> hints = new LinkedHashMap<>();
        addHints(hints, "date", properties.getFieldTypeHints().getDate());
        addHints(hints, "currency", properties.getFieldTypeHints().getCurrency());
        addHints(hints, "numeric", properties.getFieldTypeHints().getNumeric());
        return hints;
    }

    private static void addHints(Map<String, String> hints, String type, List<String> tokens) {
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                hints.put(normalizeKey(token), type);
            }
        }
    }

    static String normalizeKey(String value) {
        return NormalizationTextUtils.normalizeKey(value);
    }
}
