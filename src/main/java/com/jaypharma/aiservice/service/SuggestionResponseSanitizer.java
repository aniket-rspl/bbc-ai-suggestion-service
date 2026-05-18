package com.jaypharma.aiservice.service;

import com.jaypharma.aiservice.dto.AiSuggestionRequest;
import com.jaypharma.aiservice.dto.AiSuggestionResponse;
import com.jaypharma.aiservice.dto.SuggestionItemDto;
import com.jaypharma.aiservice.dto.TargetItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SuggestionResponseSanitizer {

    private final String promptVersion;

    public SuggestionResponseSanitizer(
            @Value("${bbc.ai.prompt-version:v1-direct-llm}") String promptVersion
    ) {
        this.promptVersion = promptVersion;
    }

    public AiSuggestionResponse sanitize(
            AiSuggestionRequest request,
            AiSuggestionResponse response
    ) {
        Set<String> sourceItems = new LinkedHashSet<>(request.sourceItems());

        Map<String, TargetItemDto> targetMap = request.targetItems()
                .stream()
                .collect(Collectors.toMap(
                        TargetItemDto::key,
                        target -> target,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, SuggestionItemDto> suggestionMap = response.suggestions() == null
                ? Map.of()
                : response.suggestions()
                  .stream()
                  .filter(item -> sourceItems.contains(item.sourceItem()))
                  .collect(Collectors.toMap(
                          SuggestionItemDto::sourceItem,
                          item -> sanitizeItem(item, targetMap),
                          (existing, duplicate) -> existing,
                          LinkedHashMap::new
                  ));

        List<SuggestionItemDto> finalSuggestions = new ArrayList<>();

        for (String sourceItem : sourceItems) {
            SuggestionItemDto item = suggestionMap.get(sourceItem);

            if (item == null) {
                item = new SuggestionItemDto(
                        sourceItem,
                        null,
                        null,
                        "LOW",
                        "No suggestion returned by AI for this item.",
                        List.of(),
                        true,
                        "Manual mapping required."
                );
            }

            finalSuggestions.add(item);
        }

        return new AiSuggestionResponse(
                request.module().name(),
                promptVersion,
                finalSuggestions
        );
    }

    private SuggestionItemDto sanitizeItem(
            SuggestionItemDto item,
            Map<String, TargetItemDto> targetMap
    ) {
        String selectedKey = item.suggestedTargetKey();

        if (selectedKey != null && !targetMap.containsKey(selectedKey)) {
            selectedKey = null;
        }

        String selectedName = selectedKey == null
                ? null
                : targetMap.get(selectedKey).name();

        String finalSelectedKey = selectedKey;
        List<String> alternatives = item.alternatives() == null
                ? List.of()
                : item.alternatives()
                  .stream()
                  .filter(targetMap::containsKey)
                  .filter(key -> !key.equals(finalSelectedKey))
                  .distinct()
                  .limit(3)
                  .toList();

        String confidenceBand = normalizeConfidenceBand(item.confidenceBand());

        boolean warningRequired = Boolean.TRUE.equals(item.warningRequired())
                || "LOW".equals(confidenceBand)
                || selectedKey == null;

        String warningMessage = warningRequired
                ? item.warningMessage() != null
                  ? item.warningMessage()
                  : "Please review this mapping manually."
                : null;

        return new SuggestionItemDto(
                item.sourceItem(),
                selectedKey,
                selectedName,
                confidenceBand,
                item.reason(),
                alternatives,
                warningRequired,
                warningMessage
        );
    }

    private String normalizeConfidenceBand(String confidenceBand) {
        if (confidenceBand == null) {
            return "LOW";
        }

        return switch (confidenceBand.toUpperCase()) {
            case "HIGH", "MEDIUM", "LOW" -> confidenceBand.toUpperCase();
            default -> "LOW";
        };
    }
}