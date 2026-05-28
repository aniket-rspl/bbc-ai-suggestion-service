package com.jaypharma.aiservice.dto.model;

import com.jaypharma.aiservice.dto.module.LearningDecisionType;
import com.jaypharma.aiservice.dto.module.SuggestionSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record MappingDecisionItemDto(

        @NotBlank(message = "sourceItem is required")
        String sourceItem,

        @NotNull(message = "decisionType is required")
        LearningDecisionType decisionType,

        SuggestionSource suggestionSource,

        String originalSuggestedTargetKey,

        String originalSuggestedTargetName,

        @NotBlank(message = "finalTargetKey is required")
        @JsonAlias("acceptedTargetKey")
        String finalTargetKey,

        @NotBlank(message = "finalTargetName is required")
        @JsonAlias("acceptedTargetName")
        String finalTargetName,

        String confidenceBand,

        String reason,

        List<String> alternatives,

        Boolean warningRequired,

        String warningMessage
) {
}
