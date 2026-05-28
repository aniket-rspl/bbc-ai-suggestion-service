package com.jaypharma.aiservice.service.orchestration;

import com.jaypharma.aiservice.dto.model.SuggestionItemDto;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionResponse;
import com.jaypharma.aiservice.service.deterministic.DeterministicMappingService;
import com.jaypharma.aiservice.service.llm.DirectLlmSuggestionService;
import com.jaypharma.aiservice.utility.SuggestionResponseSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSuggestionOrchestrator {

    private final DeterministicMappingService deterministicMappingService;
    private final DirectLlmSuggestionService directLlmSuggestionService;
    private final SuggestionResponseSanitizer sanitizer;

    public AiSuggestionResponse suggest(AiSuggestionRequest request) {
        Map<String, SuggestionItemDto> deterministicBySource = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();

        for (String sourceItem : request.sourceItems()) {
            Optional<SuggestionItemDto> deterministic = deterministicMappingService.matchSourceItem(
                    sourceItem,
                    request.targetItems()
            );
            if (deterministic.isPresent()) {
                deterministicBySource.put(sourceItem, deterministic.get());
            } else {
                unresolved.add(sourceItem);
            }
        }

        log.info(
                "Suggestion orchestration partition. module={}, total={}, deterministic={}, unresolved={}",
                request.module(),
                request.sourceItems().size(),
                deterministicBySource.size(),
                unresolved.size()
        );

        Map<String, SuggestionItemDto> llmBySource = new LinkedHashMap<>();
        if (!unresolved.isEmpty()) {
            AiSuggestionRequest reducedRequest = new AiSuggestionRequest(
                    request.borrowerId(),
                    request.borrowerName(),
                    request.collateralType(),
                    request.fileCategory(),
                    request.workbookName(),
                    request.module(),
                    List.copyOf(unresolved),
                    request.targetItems()
            );
            AiSuggestionResponse llmResponse = directLlmSuggestionService.suggestInternal(reducedRequest);
            if (llmResponse != null && llmResponse.suggestions() != null) {
                for (SuggestionItemDto item : llmResponse.suggestions()) {
                    llmBySource.put(item.sourceItem(), item);
                }
            }
        }

        List<SuggestionItemDto> merged = new ArrayList<>();
        for (String sourceItem : request.sourceItems()) {
            SuggestionItemDto item = deterministicBySource.get(sourceItem);
            if (item == null) {
                item = llmBySource.get(sourceItem);
            }
            if (item != null) {
                merged.add(item);
            }
        }

        AiSuggestionResponse mergedResponse = new AiSuggestionResponse(
                request.module().name(),
                null,
                merged
        );

        return sanitizer.sanitize(request, mergedResponse);
    }
}
