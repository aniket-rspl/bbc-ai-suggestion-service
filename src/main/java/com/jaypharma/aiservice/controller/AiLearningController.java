package com.jaypharma.aiservice.controller;

import com.jaypharma.aiservice.dto.rag.AiLearningRequest;
import com.jaypharma.aiservice.dto.rag.AiLearningResponse;
import com.jaypharma.aiservice.service.learning.UnifiedMappingLearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/learning")
@RequiredArgsConstructor
public class AiLearningController {

    private final UnifiedMappingLearningService unifiedMappingLearningService;

    @PostMapping("/mapping-decisions")
    public AiLearningResponse learnMappingDecisions(
            @Valid @RequestBody AiLearningRequest request
    ) {
        return unifiedMappingLearningService.learnMappingDecisions(request);
    }

    /**
     * @deprecated Use {@link #learnMappingDecisions(AiLearningRequest)} instead.
     */
    @Deprecated
    @PostMapping("/approved-mappings")
    public AiLearningResponse ingestApprovedMappings(
            @Valid @RequestBody AiLearningRequest request
    ) {
        return unifiedMappingLearningService.learnMappingDecisions(request);
    }
}
