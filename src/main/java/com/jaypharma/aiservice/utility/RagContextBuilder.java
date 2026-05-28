package com.jaypharma.aiservice.utility;

import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import com.jaypharma.aiservice.service.rag.HistoricalMappingRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagContextBuilder {

    private static final int MAX_HINTS_PER_SOURCE_ITEM = 2;

    private final HistoricalMappingRetriever historicalMappingRetriever;

    public String build(AiSuggestionRequest request) {
        Map<String, List<Document>> groupedDocuments =
                historicalMappingRetriever.retrieveSimilarMappingsBySourceItem(request);

        if (groupedDocuments.isEmpty()) {
            return "No approved historical mapping hints found.";
        }

        StringBuilder context = new StringBuilder();

        context.append("""
                Approved historical mapping hints:
                Use these only as guidance. Admin approval is mandatory.
                Do not use a target key unless it exists in the current targetItems.

                """);

        groupedDocuments.forEach((currentSourceItem, documents) -> {
            if (documents == null || documents.isEmpty()) {
                return;
            }

            documents.stream()
                    .limit(MAX_HINTS_PER_SOURCE_ITEM)
                    .forEach(document -> appendHint(context, currentSourceItem, document));
        });

        if (context.toString().trim().equals("Approved historical mapping hints:\nUse these only as guidance. Admin approval is mandatory.\nDo not use a target key unless it exists in the current targetItems.")) {
            return "No approved historical mapping hints found.";
        }

        return context.toString();
    }

    private void appendHint(
            StringBuilder context,
            String currentSourceItem,
            Document document
    ) {
        Map<String, Object> metadata = document.getMetadata();

        String learnedSourceItem = value(metadata, "sourceItem");

        String acceptedTargetKey = firstAvailableValue(
                metadata,
                "finalTargetKey",
                "acceptedTargetKey",
                "targetKey"
        );

        String acceptedTargetName = firstAvailableValue(
                metadata,
                "finalTargetName",
                "acceptedTargetName",
                "targetName"
        );

        String decisionType = value(metadata, "decisionType");

        if (isBlank(learnedSourceItem) || isBlank(acceptedTargetKey)) {
            return;
        }

        context.append("- Current source item \"")
                .append(currentSourceItem)
                .append("\" is similar to learned \"")
                .append(learnedSourceItem)
                .append("\" -> ")
                .append(acceptedTargetKey);

        if (!isBlank(acceptedTargetName)) {
            context.append(" (")
                    .append(acceptedTargetName)
                    .append(")");
        }

        if (!isBlank(decisionType)) {
            context.append(" [")
                    .append(decisionType)
                    .append("]");
        }

        context.append(".\n");
    }

    private String firstAvailableValue(
            Map<String, Object> metadata,
            String... keys
    ) {
        for (String key : keys) {
            String keyValue = value(metadata, key);
            if (!isBlank(keyValue)) {
                return keyValue;
            }
        }
        return "";
    }

    private String value(Map<String, Object> metadata, String key) {
        if (metadata == null || key == null) {
            return "";
        }

        Object value = metadata.get(key);

        if (value == null) {
            return "";
        }

        String stringValue = String.valueOf(value).trim();

        if ("N/A".equalsIgnoreCase(stringValue)) {
            return "";
        }

        return stringValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}