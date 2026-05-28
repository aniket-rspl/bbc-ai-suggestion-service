package com.jaypharma.aiservice.service.rag;

import com.jaypharma.aiservice.dto.model.TargetItemDto;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalMappingRetriever {

    private static final String DOCUMENT_TYPE_APPROVED_MAPPING = "APPROVED_MAPPING";
    private static final int TOP_K_PER_SOURCE_ITEM = 2;
    private static final double SIMILARITY_THRESHOLD = 0.55;

    private final VectorStore vectorStore;

    public Map<String, List<Document>> retrieveSimilarMappingsBySourceItem(AiSuggestionRequest request) {
        Map<String, List<Document>> result = new LinkedHashMap<>();

        for (String sourceItem : request.sourceItems()) {
            String query = buildSourceItemRetrievalQuery(request, sourceItem);

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(TOP_K_PER_SOURCE_ITEM)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest)
                    .stream()
                    .filter(this::isApprovedMappingDocument)
                    .filter(document -> isSameModule(document, request))
                    .toList();

            result.put(sourceItem, documents);

            log.debug(
                    "Retrieved historical mappings for sourceItem. module={}, sourceItem={}, resultCount={}",
                    request.module(),
                    sourceItem,
                    documents.size()
            );
        }

        int totalRetrieved = result.values()
                .stream()
                .mapToInt(List::size)
                .sum();

        log.info(
                "Retrieved historical mappings by source item. module={}, sourceItemCount={}, totalRetrieved={}",
                request.module(),
                request.sourceItems().size(),
                totalRetrieved
        );

        return result;
    }

    private String buildSourceItemRetrievalQuery(AiSuggestionRequest request, String sourceItem) {
        return """
            Module: %s
            Collateral Type: %s
            File Category: %s
            Source Item: %s

            Find approved historical mappings with similar source item meaning,
            abbreviation, business usage, or column naming pattern.
            """.formatted(
                request.module(),
                safe(request.collateralType()),
                safe(request.fileCategory()),
                safe(sourceItem)
        );
    }

    private boolean isApprovedMappingDocument(Document document) {
        Object documentType = document.getMetadata().get("documentType");
        return Objects.equals(DOCUMENT_TYPE_APPROVED_MAPPING, documentType);
    }

    private boolean isSameModule(Document document, AiSuggestionRequest request) {
        Object module = document.getMetadata().get("module");
        return Objects.equals(request.module().name(), module);
    }

    private String formatTargetItems(List<TargetItemDto> targetItems) {
        if (targetItems == null || targetItems.isEmpty()) {
            return "N/A";
        }

        return targetItems.stream()
                .map(target -> "%s - %s - %s".formatted(
                        safe(target.key()),
                        safe(target.name()),
                        safe(target.description())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}