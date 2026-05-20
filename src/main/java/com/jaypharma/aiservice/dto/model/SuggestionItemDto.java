package com.jaypharma.aiservice.dto.model;

import java.util.List;

public record SuggestionItemDto(
        String sourceItem,
        String suggestedTargetKey,
        String suggestedTargetName,
        String confidenceBand,
        String reason,
        List<String> alternatives,
        Boolean warningRequired,
        String warningMessage
) {
}