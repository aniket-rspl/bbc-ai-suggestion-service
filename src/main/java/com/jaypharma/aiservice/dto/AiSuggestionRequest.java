package com.jaypharma.aiservice.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiSuggestionRequest(

        Long borrowerId,

        String borrowerName,

        String collateralType,

        String fileCategory,

        String workbookName,

        @NotNull(message = "module is required")
        SuggestionModule module,

        @NotEmpty(message = "sourceItems cannot be empty")
        List<String> sourceItems,

        @NotEmpty(message = "targetItems cannot be empty")
        List<TargetItemDto> targetItems
) {
}