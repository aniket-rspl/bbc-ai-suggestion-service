package com.jaypharma.aiservice.service.deterministic;

import com.jaypharma.aiservice.dto.model.SuggestionItemDto;
import com.jaypharma.aiservice.dto.model.TargetItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DeterministicMappingServiceTest {

    @Autowired
    private DeterministicMappingService deterministicMappingService;

    private final List<TargetItemDto> targets = List.of(
            new TargetItemDto("DOCUMENT_NUMBER", "Document Number", "TEXT", "Unique document identifier"),
            new TargetItemDto("CUSTOMER_NAME", "Customer Name", "TEXT", "Customer name"),
            new TargetItemDto("CREDIT_AMOUNT", "Credit Amount", "DECIMAL", "Credit amount"),
            new TargetItemDto("OPENING_AMOUNT", "Opening Amount", "DECIMAL", "Opening amount")
    );

    @Test
    void shouldMatchDocumentNumberDeterministically() {
        Optional<SuggestionItemDto> result = deterministicMappingService.matchSourceItem("Doc #", targets);

        assertTrue(result.isPresent());
        assertEquals("DOCUMENT_NUMBER", result.get().suggestedTargetKey());
        assertEquals("HIGH", result.get().confidenceBand());
    }

    @Test
    void shouldMatchCustomerNameDeterministically() {
        Optional<SuggestionItemDto> result = deterministicMappingService.matchSourceItem("Cust Name", targets);

        assertTrue(result.isPresent());
        assertEquals("CUSTOMER_NAME", result.get().suggestedTargetKey());
    }

    @Test
    void shouldNotResolveAmbiguousCrDeterministically() {
        Optional<SuggestionItemDto> result = deterministicMappingService.matchSourceItem("CR", targets);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldNotResolveAmbiguousRemarksDeterministically() {
        Optional<SuggestionItemDto> result = deterministicMappingService.matchSourceItem("Remarks", targets);

        assertFalse(result.isPresent());
    }
}
