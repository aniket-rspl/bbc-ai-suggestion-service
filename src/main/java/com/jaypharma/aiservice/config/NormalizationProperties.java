package com.jaypharma.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "bbc.ai.normalization")
public class NormalizationProperties {

    private List<KeywordGroup> keywordGroups = new ArrayList<>();
    private List<AmbiguousKeywordGroup> ambiguousKeywordGroups = new ArrayList<>();
    private List<String> cleanedPhaseTokens = new ArrayList<>();
    private List<String> noiseTokens = new ArrayList<>();
    private List<String> vagueSourcePatterns = new ArrayList<>();
    private FieldTypeHints fieldTypeHints = new FieldTypeHints();
    private Thresholds thresholds = new Thresholds();
    private Messages messages = new Messages();

    @Data
    public static class KeywordGroup {
        private String canonical;
        private List<String> aliases = new ArrayList<>();
    }

    @Data
    public static class AmbiguousKeywordGroup {
        private String canonical;
        private List<String> aliases = new ArrayList<>();
        private String ambiguityReason;
    }

    @Data
    public static class FieldTypeHints {
        private List<String> date = new ArrayList<>();
        private List<String> currency = new ArrayList<>();
        private List<String> numeric = new ArrayList<>();
    }

    @Data
    public static class Thresholds {
        private double high = 0.90;
        private double medium = 0.75;
        private double ambiguityGap = 0.08;
        private double exactMatchScore = 1.0;
        private double normalizedExactScore = 0.95;
        private double synonymScore = 0.92;
        private double fuzzyMinCandidateScore = 0.75;
    }

    @Data
    public static class Messages {
        private String deterministicHighReason = "Matched by deterministic {strategy}: {detail}.";
        private String deterministicMediumReason = "Possible deterministic match ({strategy}): {detail}. Review recommended.";
        private String manualReviewWarning = "Please review this mapping manually.";
        private String noSuggestionReason = "No suggestion returned for this item.";
        private String ambiguousSourceReason = "Source label is ambiguous and requires manual review.";
    }
}
