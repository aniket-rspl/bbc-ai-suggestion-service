package com.jaypharma.aiservice.service.preprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NormalizationKnowledgeStoreTest {

    @Autowired
    private NormalizationKnowledgeStore knowledgeStore;

    @Autowired
    private SourceItemPreprocessor preprocessor;

    @Test
    void shouldUpsertAliasAndUseInPreprocessing() {
        knowledgeStore.upsertAlias("widget code", "wgt cd", "test");

        var result = preprocessor.preprocess("Wgt Cd");

        assertEquals("widget code", result.normalizedValue());
        assertTrue(knowledgeStore.resolveCanonicalPhrase("wgt cd").isPresent());
        assertEquals("widget code", knowledgeStore.resolveCanonicalPhrase("wgt cd").orElse(""));
    }
}
