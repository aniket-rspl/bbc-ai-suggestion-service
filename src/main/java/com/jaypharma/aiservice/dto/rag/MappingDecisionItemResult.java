package com.jaypharma.aiservice.dto.rag;

import java.util.List;

public record MappingDecisionItemResult(
        String sourceItem,
        String decisionType,
        String status,
        String vectorLearningId,
        boolean aliasUpdated,
        String canonicalPhrase,
        List<String> aliasesAdded,
        String errorMessage
) {
}
