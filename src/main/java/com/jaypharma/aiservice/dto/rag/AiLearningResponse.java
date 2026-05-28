package com.jaypharma.aiservice.dto.rag;

import java.util.List;

public record AiLearningResponse(
        String status,
        String module,
        int requestedCount,
        int learnedCount,
        int aliasUpdatedCount,
        int vectorIngestedCount,
        int failedCount,
        List<String> learningIds,
        List<MappingDecisionItemResult> items
) {
    public int ingestedCount() {
        return vectorIngestedCount;
    }
}
