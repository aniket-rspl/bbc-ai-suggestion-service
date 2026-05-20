package com.jaypharma.aiservice.dto.suggestion;

import com.jaypharma.aiservice.dto.model.SuggestionItemDto;

import java.util.List;

public record AiSuggestionResponse(
        String module,
        String promptVersion,
        List<SuggestionItemDto> suggestions
) {
}