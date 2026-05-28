package com.jaypharma.aiservice.service.learning;

import com.jaypharma.aiservice.dto.model.MappingDecisionItemDto;
import com.jaypharma.aiservice.dto.module.LearningDecisionType;
import com.jaypharma.aiservice.dto.module.SuggestionModule;
import com.jaypharma.aiservice.dto.module.SuggestionSource;
import com.jaypharma.aiservice.dto.rag.AiLearningRequest;
import com.jaypharma.aiservice.dto.rag.AiLearningResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UnifiedMappingLearningServiceTest {

    @Autowired
    private UnifiedMappingLearningService unifiedMappingLearningService;

    @Test
    void shouldLearnAliasAndVectorTogether() {
        AiLearningRequest request = new AiLearningRequest(
                SuggestionModule.COLUMN_MAPPING,
                99L,
                "Test Borrower",
                "AR",
                "AR Ledger",
                "test.xlsx",
                "v3-deterministic-001",
                "test-admin",
                List.of(
                        new MappingDecisionItemDto(
                                "Trx Cd",
                                LearningDecisionType.CORRECTED,
                                SuggestionSource.LLM,
                                "DOCUMENT_NUMBER",
                                "Document Number",
                                "TRANSACTION_TYPE",
                                "Transaction Type",
                                "MEDIUM",
                                "corrected by admin",
                                List.of(),
                                false,
                                null
                        )
                )
        );

        AiLearningResponse response = unifiedMappingLearningService.learnMappingDecisions(request);

        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.learnedCount());
        assertEquals(1, response.aliasUpdatedCount());
        assertEquals(1, response.vectorIngestedCount());
        assertEquals(1, response.items().size());
        assertEquals("LEARNED", response.items().get(0).status());
        assertTrue(response.items().get(0).aliasUpdated());
        assertEquals("transaction type", response.items().get(0).canonicalPhrase());
    }

    @Test
    void shouldRejectDuplicateSourceItems() {
        AiLearningRequest request = new AiLearningRequest(
                SuggestionModule.COLUMN_MAPPING,
                null,
                null,
                null,
                null,
                null,
                null,
                "admin",
                List.of(
                        new MappingDecisionItemDto(
                                "Doc #",
                                LearningDecisionType.ACCEPTED,
                                null,
                                null,
                                null,
                                "DOCUMENT_NUMBER",
                                "Document Number",
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        new MappingDecisionItemDto(
                                "Doc #",
                                LearningDecisionType.CORRECTED,
                                null,
                                null,
                                null,
                                "DOCUMENT_NUMBER",
                                "Document Number",
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                unifiedMappingLearningService.learnMappingDecisions(request)
        );
    }
}
