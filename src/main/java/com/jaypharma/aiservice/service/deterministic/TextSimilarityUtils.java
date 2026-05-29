package com.jaypharma.aiservice.service.deterministic;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

public final class TextSimilarityUtils {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();

    private TextSimilarityUtils() {
    }

    /**
     * Returns a similarity score in the range [0.0, 1.0] using the stronger of
     * Jaro-Winkler and normalized Levenshtein ratio.
     */
    public static double similarity(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }

        double jaroWinklerScore = JARO_WINKLER.apply(left, right);
        double levenshteinScore = levenshteinSimilarity(left, right);
        return Math.max(jaroWinklerScore, levenshteinScore);
    }

    private static double levenshteinSimilarity(String left, String right) {
        int distance = LEVENSHTEIN.apply(left, right);
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - ((double) distance / maxLength);
    }
}
