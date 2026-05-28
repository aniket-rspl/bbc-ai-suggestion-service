package com.jaypharma.aiservice.service.orchestration;

import com.jaypharma.aiservice.dto.model.SuggestionItemDto;
import com.jaypharma.aiservice.dto.model.TargetItemDto;
import com.jaypharma.aiservice.dto.module.SuggestionModule;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionRequest;
import com.jaypharma.aiservice.dto.suggestion.AiSuggestionResponse;
import com.jaypharma.aiservice.service.llm.DirectLlmSuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class AiSuggestionOrchestratorTest {

    @Autowired
    private AiSuggestionOrchestrator orchestrator;

    @MockitoBean
    private DirectLlmSuggestionService directLlmSuggestionService;

    private final List<TargetItemDto> targets = List.of(
            new TargetItemDto("DOCUMENT_NUMBER", "Document Number", "TEXT", "Document number"),
            new TargetItemDto("TRANSACTION_TYPE", "Transaction Type", "TEXT", "Transaction type"),
            new TargetItemDto("CUSTOMER_NAME", "Customer Name", "TEXT", "Customer name"),
            new TargetItemDto("INVOICE_DATE", "Invoice Date", "DATE", "Invoice date"),
            new TargetItemDto("DUE_DATE", "Due Date", "DATE", "Due date"),
            new TargetItemDto("OPENING_AMOUNT", "Opening Amount", "DECIMAL", "Opening amount"),
            new TargetItemDto("CREDIT_AMOUNT", "Credit Amount", "DECIMAL", "Credit amount"),
            new TargetItemDto("OUTSTANDING_AMOUNT", "Outstanding Amount", "DECIMAL", "Outstanding amount")
    );

    @Test
    void shouldCallLlmOnlyForUnresolvedItems() {
        AiSuggestionRequest request = new AiSuggestionRequest(
                401L,
                "Delta Foods",
                "AR",
                "AR Ledger",
                "delta-ar-detail.xlsx",
                SuggestionModule.COLUMN_MAPPING,
                List.of(
                        "Doc #",
                        "Txn Type",
                        "Cust Name",
                        "Inv Dt",
                        "Due Dt",
                        "Beg Bal",
                        "Credit Amt",
                        "Remaining Balance",
                        "CR",
                        "Remarks",
                        "Invt"
                ),
                targets
        );

        when(directLlmSuggestionService.suggestInternal(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiSuggestionResponse(
                SuggestionModule.COLUMN_MAPPING.name(),
                "v3-deterministic-001",
                List.of(
                        new SuggestionItemDto("CR", "CREDIT_AMOUNT", "Credit Amount", "MEDIUM", "LLM", List.of(), false, null),
                        new SuggestionItemDto("Remarks", null, null, "LOW", "LLM", List.of(), true, "Review"),
                        new SuggestionItemDto("Invt", null, null, "LOW", "LLM", List.of(), true, "Review")
                )
        ));

        AiSuggestionResponse response = orchestrator.suggest(request);

        verify(directLlmSuggestionService).suggestInternal(argThat(reduced ->
                reduced.sourceItems().size() == 3
                        && reduced.sourceItems().contains("CR")
                        && reduced.sourceItems().contains("Remarks")
                        && reduced.sourceItems().contains("Invt")
        ));
        assertEquals(11, response.suggestions().size());
        assertEquals("DOCUMENT_NUMBER", findSuggestion(response, "Doc #").suggestedTargetKey());
        assertEquals("CUSTOMER_NAME", findSuggestion(response, "Cust Name").suggestedTargetKey());
        assertEquals("CREDIT_AMOUNT", findSuggestion(response, "CR").suggestedTargetKey());
    }

    private SuggestionItemDto findSuggestion(AiSuggestionResponse response, String sourceItem) {
        return response.suggestions().stream()
                .filter(item -> item.sourceItem().equals(sourceItem))
                .findFirst()
                .orElseThrow();
    }
}
