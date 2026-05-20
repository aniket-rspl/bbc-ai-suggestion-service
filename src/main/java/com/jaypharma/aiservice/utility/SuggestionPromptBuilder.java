package com.jaypharma.aiservice.utility;

import com.jaypharma.aiservice.dto.model.TargetItemDto;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SuggestionPromptBuilder {

    public String build(AiSuggestionRequest request, String ragContext) {
        return switch (request.module()) {
            case COLUMN_MAPPING -> buildColumnMappingPrompt(request, ragContext);
            case SHEET_MAPPING -> buildSheetMappingPrompt(request, ragContext);
        };
    }

    public String build(AiSuggestionRequest request) {
        return build(request, "No historical RAG context available.");
    }

    private String buildColumnMappingPrompt(AiSuggestionRequest request, String ragContext) {
        return """
                Generate column-to-canonical-field mapping suggestions.

                Historical approved mapping context:
                %s

                Historical mapping usage rules:
                - Historical mappings are guidance only.
                - Historical mappings are not source of truth.
                - Do not copy a historical mapping blindly.
                - Use historical mappings only when they are relevant to the current source item and current target items.
                - If a historical mapping uses a target key that is not present in the current request targetItems, do not use that target key.
                - Admin approval is mandatory.

                Current request context:
                Borrower Name: %s
                Collateral Type: %s
                File Category: %s
                Workbook Name: %s

                Source items are uploaded file headers.
                Target items are canonical fields.

                Source Items:
                %s

                Target Items:
                %s

                Column mapping instructions:
                - sourceItem must exactly match one value from Source Items.
                - suggestedTargetKey must be one of Target Items key values, or null.
                - suggestedTargetName must match the selected target item name, or null.
                - alternatives must contain only Target Items key values.
                - Return exactly one suggestion for each source item.
                - Do not invent source items.
                - Do not invent target items.
                - If no suitable mapping exists, return null target key and LOW confidence.
                - Use HIGH confidence only when the meaning is clear.
                - Use MEDIUM confidence when there is a reasonable match but ambiguity exists.
                - Use LOW confidence when the item is unclear or no reliable match exists.
                - warningRequired should be true for LOW confidence or null target mappings.
                - warningMessage should explain that manual review is required when warningRequired is true.

                Required JSON property names:
                - sourceItem
                - suggestedTargetKey
                - suggestedTargetName
                - confidenceBand
                - reason
                - alternatives
                - warningRequired
                - warningMessage

                Return JSON only in this exact shape:
                {
                  "module": "COLUMN_MAPPING",
                  "promptVersion": "v2-rag-001",
                  "suggestions": [
                    {
                      "sourceItem": "exact source item",
                      "suggestedTargetKey": "target key or null",
                      "suggestedTargetName": "target name or null",
                      "confidenceBand": "HIGH/MEDIUM/LOW",
                      "reason": "brief reason",
                      "alternatives": [],
                      "warningRequired": false,
                      "warningMessage": null
                    }
                  ]
                }
                """.formatted(
                safe(ragContext),
                safe(request.borrowerName()),
                safe(request.collateralType()),
                safe(request.fileCategory()),
                safe(request.workbookName()),
                formatSourceItems(request.sourceItems()),
                formatTargetItems(request.targetItems())
        );
    }

    private String buildSheetMappingPrompt(AiSuggestionRequest request, String ragContext) {
        return """
                Generate sheet-to-file-category mapping suggestions.

                Historical approved mapping context:
                %s

                Historical mapping usage rules:
                - Historical mappings are guidance only.
                - Historical mappings are not source of truth.
                - Do not copy a historical mapping blindly.
                - Use historical mappings only when they are relevant to the current source item and current target items.
                - If a historical mapping uses a target key that is not present in the current request targetItems, do not use that target key.
                - Admin approval is mandatory.

                Current request context:
                Borrower Name: %s
                Collateral Type: %s
                Workbook Name: %s

                Source items are workbook sheet names.
                Target items are valid file categories or collateral report categories.

                Source Items:
                %s

                Target Items:
                %s

                Sheet mapping instructions:
                - sourceItem must exactly match one value from Source Items.
                - suggestedTargetKey must be one of Target Items key values, or null.
                - suggestedTargetName must match the selected target item name, or null.
                - alternatives must contain only Target Items key values.
                - Return exactly one suggestion for each source item.
                - Do not invent source items.
                - Do not invent target items.
                - If no suitable mapping exists, return null target key and LOW confidence.
                - Use HIGH confidence only when the meaning is clear.
                - Use MEDIUM confidence when there is a reasonable match but ambiguity exists.
                - Use LOW confidence when the item is unclear or no reliable match exists.
                - warningRequired should be true for LOW confidence or null target mappings.
                - warningMessage should explain that manual review is required when warningRequired is true.

                Required JSON property names:
                - sourceItem
                - suggestedTargetKey
                - suggestedTargetName
                - confidenceBand
                - reason
                - alternatives
                - warningRequired
                - warningMessage

                Do not use old property names:
                - sourceColumn
                - sheetName
                - suggestedCanonicalFieldKey
                - suggestedCanonicalFieldName
                - suggestedCategoryKey
                - suggestedCategoryName

                Return JSON only in this exact shape:
                {
                  "module": "SHEET_MAPPING",
                  "promptVersion": "v2-rag-001",
                  "suggestions": [
                    {
                      "sourceItem": "exact source item",
                      "suggestedTargetKey": "target key or null",
                      "suggestedTargetName": "target name or null",
                      "confidenceBand": "HIGH/MEDIUM/LOW",
                      "reason": "brief reason",
                      "alternatives": [],
                      "warningRequired": false,
                      "warningMessage": null
                    }
                  ]
                }
                """.formatted(
                safe(ragContext),
                safe(request.borrowerName()),
                safe(request.collateralType()),
                safe(request.workbookName()),
                formatSourceItems(request.sourceItems()),
                formatTargetItems(request.targetItems())
        );
    }

    private String formatSourceItems(List<String> sourceItems) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return "- No source items provided";
        }

        return sourceItems.stream()
                .map(item -> "- " + safe(item))
                .collect(Collectors.joining("\n"));
    }

    private String formatTargetItems(List<TargetItemDto> targetItems) {
        if (targetItems == null || targetItems.isEmpty()) {
            return "- No target items provided";
        }

        return targetItems.stream()
                .map(target -> """
                        - key: %s
                          name: %s
                          dataType: %s
                          description: %s
                        """.formatted(
                        safe(target.key()),
                        safe(target.name()),
                        safe(target.dataType()),
                        safe(target.description())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}