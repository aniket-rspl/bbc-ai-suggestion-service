package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.dto.model.MappingDecisionItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizationLearningService {

    private final NormalizationKnowledgeStore knowledgeStore;

    /**
     * Updates alias/normalization store for every accepted or corrected decision.
     *
     * @return canonical phrase used for the alias group
     */
    public String learnFromDecision(MappingDecisionItemDto item) {
        String canonicalPhrase = knowledgeStore.upsertFromApprovedMapping(
                item.sourceItem(),
                item.finalTargetKey(),
                item.finalTargetName()
        );

        log.debug(
                "Normalization knowledge updated. sourceItem={}, finalTargetKey={}, decisionType={}, canonicalPhrase={}",
                item.sourceItem(),
                item.finalTargetKey(),
                item.decisionType(),
                canonicalPhrase
        );

        return canonicalPhrase;
    }
}
