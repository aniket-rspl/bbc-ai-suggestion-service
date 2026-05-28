package com.jaypharma.aiservice.service.preprocessing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface NormalizationKnowledgeStore {

    Optional<String> resolveCanonicalPhrase(String normalizedPhrase);

    Optional<String> resolveAliasToken(String token);

    Map<String, Set<String>> getKeywordGroups();

    List<AmbiguousEntry> getAmbiguousEntries();

    List<String> getCleanedPhaseTokens();

    List<String> getNoiseTokens();

    List<String> getVagueSourcePatterns();

    NormalizationThresholds getThresholds();

    NormalizationMessages getMessages();

    List<String> getFieldTypeHintTokens(String fieldType);

    void upsertAlias(String canonical, String alias, String source);

    /**
     * @return canonical phrase used for the alias group, or empty if nothing was stored
     */
    String upsertFromApprovedMapping(
            String sourceItem,
            String finalTargetKey,
            String finalTargetName
    );

    record AmbiguousEntry(String canonical, Set<String> aliases, String ambiguityReason) {}

    record NormalizationThresholds(
            double high,
            double medium,
            double ambiguityGap,
            double exactMatchScore,
            double normalizedExactScore,
            double synonymScore
    ) {}

    record NormalizationMessages(
            String deterministicHighReason,
            String deterministicMediumReason,
            String manualReviewWarning,
            String noSuggestionReason,
            String ambiguousSourceReason
    ) {}
}
