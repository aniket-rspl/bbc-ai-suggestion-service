package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.dto.module.DetectedFieldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SourceItemPreprocessorTest {

    @Autowired
    private SourceItemPreprocessor preprocessor;

    @Test
    void shouldNormalizeDocumentNumber() {
        var result = preprocessor.preprocess("Doc #");

        assertEquals("doc number", result.cleanedValue());
        assertEquals("document number", result.normalizedValue());
        assertEquals(DetectedFieldType.TEXT, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeTransactionType() {
        var result = preprocessor.preprocess("Txn Type");

        assertEquals("transaction type", result.normalizedValue());
        assertEquals(DetectedFieldType.TEXT, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeCustomerName() {
        var result = preprocessor.preprocess("Cust Name");

        assertEquals("customer name", result.normalizedValue());
        assertEquals(DetectedFieldType.TEXT, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeInvoiceDate() {
        var result = preprocessor.preprocess("Inv Dt");

        assertEquals("invoice date", result.normalizedValue());
        assertEquals(DetectedFieldType.DATE, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeDueDate() {
        var result = preprocessor.preprocess("Due Dt");

        assertEquals("due date", result.normalizedValue());
        assertEquals(DetectedFieldType.DATE, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeBeginningBalance() {
        var result = preprocessor.preprocess("Beg Balance");

        assertEquals("beginning balance", result.normalizedValue());
        assertEquals(DetectedFieldType.CURRENCY, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldNormalizeCreditAmount() {
        var result = preprocessor.preprocess("Credit Amt");

        assertEquals("credit amount", result.normalizedValue());
        assertEquals(DetectedFieldType.CURRENCY, result.detectedFieldType());
        assertFalse(result.ambiguous());
    }

    @Test
    void shouldMarkCrAsAmbiguousEvenIfExpanded() {
        var result = preprocessor.preprocess("CR");

        assertEquals("credit", result.normalizedValue());
        assertEquals(DetectedFieldType.CURRENCY, result.detectedFieldType());
        assertTrue(result.ambiguous());
        assertFalse(result.ambiguityReasons().isEmpty());
    }

    @Test
    void shouldMarkRemarksAsAmbiguous() {
        var result = preprocessor.preprocess("Remarks");

        assertEquals("remark", result.normalizedValue());
        assertTrue(result.ambiguous());
        assertFalse(result.ambiguityReasons().isEmpty());
    }

    @Test
    void shouldMarkInvtAsAmbiguous() {
        var result = preprocessor.preprocess("Invt");

        assertEquals("inventory", result.normalizedValue());
        assertTrue(result.ambiguous());
        assertFalse(result.ambiguityReasons().isEmpty());
    }

    @Test
    void shouldSplitCamelCase() {
        var result = preprocessor.preprocess("InvoiceCreationDt");

        assertEquals("invoice creation date", result.normalizedValue());
        assertEquals(DetectedFieldType.DATE, result.detectedFieldType());
    }
}
