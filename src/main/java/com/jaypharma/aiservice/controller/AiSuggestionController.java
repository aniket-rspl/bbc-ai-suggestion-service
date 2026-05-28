package com.jaypharma.aiservice.controller;

import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionResponse;
import com.jaypharma.aiservice.service.orchestration.AiSuggestionOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/suggestions")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiSuggestionOrchestrator suggestionService;

    @PostMapping
    public AiSuggestionResponse suggest(@Valid @RequestBody AiSuggestionRequest request) {
        return suggestionService.suggest(request);
    }
}