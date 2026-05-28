package com.jaypharma.aiservice.dto.rag;

import com.jaypharma.aiservice.dto.model.MappingDecisionItemDto;
import com.jaypharma.aiservice.dto.module.SuggestionModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record AiLearningRequest(

        @NotNull(message = "module is required")
        SuggestionModule module,

        Long borrowerId,

        String borrowerName,

        String collateralType,

        String fileCategory,

        String workbookName,

        String promptVersion,

        @JsonAlias("approvedBy")
        String decidedBy,

        @NotEmpty(message = "decisions cannot be empty")
        @Valid
        @JsonAlias("suggestions")
        List<MappingDecisionItemDto> decisions
) {
    public String approvedBy() {
        return decidedBy;
    }

    public List<MappingDecisionItemDto> suggestions() {
        return decisions;
    }
}
