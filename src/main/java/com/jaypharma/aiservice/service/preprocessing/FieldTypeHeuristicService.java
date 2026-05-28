package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.dto.module.DetectedFieldType;
import org.springframework.stereotype.Service;

@Service
public class FieldTypeHeuristicService {

    private final NormalizationKnowledgeStore knowledgeStore;

    public FieldTypeHeuristicService(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    public DetectedFieldType detect(String normalizedValue) {
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return DetectedFieldType.UNKNOWN;
        }
        String normalized = NormalizationTextUtils.normalizeKey(normalizedValue);

        if (containsAnyHint(normalized, knowledgeStore.getFieldTypeHintTokens("date"))) {
            return DetectedFieldType.DATE;
        }
        if (containsAnyHint(normalized, knowledgeStore.getFieldTypeHintTokens("currency"))) {
            return DetectedFieldType.CURRENCY;
        }
        if (containsAnyHint(normalized, knowledgeStore.getFieldTypeHintTokens("numeric"))) {
            return DetectedFieldType.NUMERIC;
        }
        return DetectedFieldType.TEXT;
    }

    private boolean containsAnyHint(String normalized, java.util.List<String> hints) {
        java.util.List<String> tokens = java.util.Arrays.asList(normalized.split(" "));
        for (String hint : hints) {
            if (hint.contains(" ")) {
                if (normalized.contains(hint)) {
                    return true;
                }
            } else if (tokens.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
