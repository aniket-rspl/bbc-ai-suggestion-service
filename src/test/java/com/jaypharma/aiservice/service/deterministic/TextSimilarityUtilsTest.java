package com.jaypharma.aiservice.service.deterministic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSimilarityUtilsTest {

    @Test
    void shouldReturnOneForIdenticalStrings() {
        assertEquals(1.0, TextSimilarityUtils.similarity("document number", "document number"));
    }

    @Test
    void shouldScoreTypoAboveThreshold() {
        double score = TextSimilarityUtils.similarity("document numbr", "document number");
        assertTrue(score >= 0.90, "Expected typo similarity >= 0.90 but was " + score);
    }

    @Test
    void shouldScoreCustomerNameTypoHighly() {
        double score = TextSimilarityUtils.similarity("customer nmae", "customer name");
        assertTrue(score >= 0.85, "Expected customer name typo similarity >= 0.85 but was " + score);
    }

    @Test
    void shouldScoreDissimilarStringsLow() {
        double score = TextSimilarityUtils.similarity("invoice date", "credit amount");
        assertTrue(score < 0.75, "Expected dissimilar score < 0.75 but was " + score);
    }
}
