package com.jaypharma.aiservice.dto;

import java.util.List;

public record AiSuggestionResponse(
        String module,
        String promptVersion,
        List<SuggestionItemDto> suggestions
) {
}