package com.jaypharma.aiservice.service.rag;

import com.jaypharma.aiservice.dto.model.MappingDecisionItemDto;
import com.jaypharma.aiservice.dto.rag.AiLearningRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreIngestionService {

    private static final String DOCUMENT_TYPE_APPROVED_MAPPING = "APPROVED_MAPPING";

    private final VectorStore vectorStore;

    public String ingestDecision(
            AiLearningRequest request,
            MappingDecisionItemDto item,
            String canonicalPhrase
    ) {
        String learningId = UUID.randomUUID().toString();
        Map<String, Object> metadata = buildMetadata(request, item, learningId, canonicalPhrase);
        String documentText = buildDocumentText(request, item, canonicalPhrase);

        Document document = new Document(learningId, documentText, metadata);
        vectorStore.add(List.of(document));

        log.debug(
                "Vector document ingested. learningId={}, sourceItem={}, finalTargetKey={}",
                learningId,
                item.sourceItem(),
                item.finalTargetKey()
        );

        return learningId;
    }

    private Map<String, Object> buildMetadata(
            AiLearningRequest request,
            MappingDecisionItemDto item,
            String learningId,
            String canonicalPhrase
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("documentType", DOCUMENT_TYPE_APPROVED_MAPPING);
        metadata.put("learningId", learningId);

        metadata.put("module", request.module().name());
        metadata.put("promptVersion", safe(request.promptVersion()));

        metadata.put("borrowerId", request.borrowerId() == null ? "N/A" : request.borrowerId());
        metadata.put("borrowerName", safe(request.borrowerName()));
        metadata.put("collateralType", safe(request.collateralType()));
        metadata.put("fileCategory", safe(request.fileCategory()));
        metadata.put("workbookName", safe(request.workbookName()));

        metadata.put("sourceItem", safe(item.sourceItem()));
        metadata.put("canonicalPhrase", safe(canonicalPhrase));
        metadata.put("originalSuggestedTargetKey", safe(item.originalSuggestedTargetKey()));
        metadata.put("originalSuggestedTargetName", safe(item.originalSuggestedTargetName()));
        metadata.put("finalTargetKey", safe(item.finalTargetKey()));
        metadata.put("finalTargetName", safe(item.finalTargetName()));
        metadata.put("acceptedTargetKey", safe(item.finalTargetKey()));
        metadata.put("acceptedTargetName", safe(item.finalTargetName()));
        metadata.put("decisionType", item.decisionType().name());
        metadata.put("suggestionSource", item.suggestionSource() == null ? "N/A" : item.suggestionSource().name());
        metadata.put("confidenceBand", safe(item.confidenceBand()));

        metadata.put("decidedBy", safe(request.decidedBy()));
        metadata.put("approvedBy", safe(request.decidedBy()));
        metadata.put("createdAt", Instant.now().toString());

        return metadata;
    }

    private String buildDocumentText(
            AiLearningRequest request,
            MappingDecisionItemDto item,
            String canonicalPhrase
    ) {
        return """
                Approved admin learning record.

                Module: %s
                Borrower Name: %s
                Collateral Type: %s
                File Category: %s
                Workbook Name: %s
                Prompt Version: %s

                Source Item: %s
                Canonical Phrase: %s

                Original Suggested Target Key: %s
                Original Suggested Target Name: %s

                Admin Final Target Key: %s
                Admin Final Target Name: %s

                Decision Type: %s
                Suggestion Source: %s
                Confidence Band: %s

                AI Reason:
                %s

                Warning Required: %s
                Warning Message: %s
                """.formatted(
                request.module(),
                safe(request.borrowerName()),
                safe(request.collateralType()),
                safe(request.fileCategory()),
                safe(request.workbookName()),
                safe(request.promptVersion()),

                safe(item.sourceItem()),
                safe(canonicalPhrase),

                safe(item.originalSuggestedTargetKey()),
                safe(item.originalSuggestedTargetName()),

                safe(item.finalTargetKey()),
                safe(item.finalTargetName()),

                item.decisionType(),
                item.suggestionSource() == null ? "N/A" : item.suggestionSource(),
                safe(item.confidenceBand()),

                safe(item.reason()),

                item.warningRequired() == null ? "false" : item.warningRequired(),
                safe(item.warningMessage())
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}
