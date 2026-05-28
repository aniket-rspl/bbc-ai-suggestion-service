package com.jaypharma.aiservice.service.learning;

import com.jaypharma.aiservice.dto.model.MappingDecisionItemDto;
import com.jaypharma.aiservice.dto.rag.AiLearningRequest;
import com.jaypharma.aiservice.dto.rag.AiLearningResponse;
import com.jaypharma.aiservice.dto.rag.MappingDecisionItemResult;
import com.jaypharma.aiservice.service.preprocessing.NormalizationLearningService;
import com.jaypharma.aiservice.service.rag.VectorStoreIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedMappingLearningService {

    private static final String STATUS_LEARNED = "LEARNED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String BATCH_COMPLETED = "COMPLETED";
    private static final String BATCH_PARTIAL = "PARTIAL";

    private final NormalizationLearningService normalizationLearningService;
    private final VectorStoreIngestionService vectorStoreIngestionService;

    public AiLearningResponse learnMappingDecisions(AiLearningRequest request) {
        validateUniqueSourceItems(request.decisions());

        List<MappingDecisionItemResult> itemResults = new ArrayList<>();
        List<String> learningIds = new ArrayList<>();
        int aliasUpdatedCount = 0;
        int vectorIngestedCount = 0;
        int failedCount = 0;

        for (MappingDecisionItemDto item : request.decisions()) {
            try {
                String canonicalPhrase = normalizationLearningService.learnFromDecision(item);
                boolean aliasUpdated = canonicalPhrase != null && !canonicalPhrase.isBlank();
                if (aliasUpdated) {
                    aliasUpdatedCount++;
                }

                String learningId = vectorStoreIngestionService.ingestDecision(request, item, canonicalPhrase);
                learningIds.add(learningId);
                vectorIngestedCount++;

                itemResults.add(new MappingDecisionItemResult(
                        item.sourceItem(),
                        item.decisionType().name(),
                        STATUS_LEARNED,
                        learningId,
                        aliasUpdated,
                        canonicalPhrase,
                        aliasUpdated ? List.of(item.sourceItem().trim()) : List.of(),
                        null
                ));
            } catch (Exception ex) {
                failedCount++;
                log.error(
                        "Failed to learn mapping decision. sourceItem={}, decisionType={}",
                        item.sourceItem(),
                        item.decisionType(),
                        ex
                );
                itemResults.add(new MappingDecisionItemResult(
                        item.sourceItem(),
                        item.decisionType().name(),
                        STATUS_FAILED,
                        null,
                        false,
                        null,
                        List.of(),
                        ex.getMessage()
                ));
            }
        }

        int learnedCount = vectorIngestedCount;
        String batchStatus = failedCount == 0 ? BATCH_COMPLETED : BATCH_PARTIAL;

        log.info(
                "Unified mapping learning completed. module={}, requested={}, learned={}, aliasUpdated={}, vectorIngested={}, failed={}, decidedBy={}",
                request.module(),
                request.decisions().size(),
                learnedCount,
                aliasUpdatedCount,
                vectorIngestedCount,
                failedCount,
                request.decidedBy()
        );

        return new AiLearningResponse(
                batchStatus,
                request.module().name(),
                request.decisions().size(),
                learnedCount,
                aliasUpdatedCount,
                vectorIngestedCount,
                failedCount,
                learningIds,
                itemResults
        );
    }

    private void validateUniqueSourceItems(List<MappingDecisionItemDto> decisions) {
        Set<String> seen = new HashSet<>();
        for (MappingDecisionItemDto item : decisions) {
            String key = item.sourceItem() == null ? "" : item.sourceItem().trim().toLowerCase();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate sourceItem in learning batch: " + item.sourceItem()
                );
            }
        }
    }
}
