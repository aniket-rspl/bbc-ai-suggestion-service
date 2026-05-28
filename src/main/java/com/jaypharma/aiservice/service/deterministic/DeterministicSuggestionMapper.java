package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.SuggestionItemDto;
import com.jaypharma.aiservice.service.preprocessing.NormalizationKnowledgeStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeterministicSuggestionMapper {

    private final NormalizationKnowledgeStore knowledgeStore;

    public DeterministicSuggestionMapper(NormalizationKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    public SuggestionItemDto toSuggestionItem(DeterministicMatchResult result) {
        String reason = formatMessage(
                knowledgeStore.getMessages().deterministicHighReason(),
                result.strategyName(),
                result.reason().isBlank() ? result.targetName() : result.reason()
        );

        return new SuggestionItemDto(
                result.sourceItem(),
                result.targetKey(),
                result.targetName(),
                result.confidence().name(),
                reason,
                result.alternatives() == null ? List.of() : List.copyOf(result.alternatives()),
                false,
                null
        );
    }

    private String formatMessage(String template, String strategy, String detail) {
        return template
                .replace("{strategy}", strategy)
                .replace("{detail}", detail);
    }
}
