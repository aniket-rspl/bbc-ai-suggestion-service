package com.jaypharma.aiservice.service;

import com.jaypharma.aiservice.dto.AiSuggestionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SuggestionPromptBuilder {

    private final String promptVersion;

    public SuggestionPromptBuilder(
            @Value("${bbc.ai.prompt-version:v1-direct-llm}") String promptVersion
    ) {
        this.promptVersion = promptVersion;
    }

    public String build(AiSuggestionRequest request) {
        return switch (request.module()) {
            case COLUMN_MAPPING -> buildColumnMappingPrompt(request);
            case SHEET_MAPPING -> buildSheetMappingPrompt(request);
        };
    }

    private String buildColumnMappingPrompt(AiSuggestionRequest request) {
        return """
                Generate column-to-canonical-field mapping suggestions.

                Borrower ID:
                %s

                Borrower Name:
                %s

                Collateral Type:
                %s

                File Category:
                %s

                Uploaded Headers:
                %s

                Canonical Fields:
                %s

                Task:
                For each uploaded header, suggest the most appropriate canonical field.

                Important:
                - sourceItem must exactly match one value from sourceItems.
                - suggestedTargetKey must be one of targetItems.key values, or null.
                - alternatives must contain only targetItems.key values.
                - Return exactly one suggestion for each source item.

                Output JSON:
                {
                  "module": "COLUMN_MAPPING",
                  "promptVersion": "%s",
                  "suggestions": [
                    {
                      "sourceItem": "Inv Dt",
                      "suggestedTargetKey": "INVOICE_DATE",
                      "suggestedTargetName": "Invoice Date",
                      "confidenceBand": "HIGH",
                      "reason": "Inv Dt appears to mean invoice date.",
                      "alternatives": [],
                      "warningRequired": false,
                      "warningMessage": null
                    }
                  ]
                }
                """.formatted(
                request.borrowerId(),
                request.borrowerName(),
                request.collateralType(),
                request.fileCategory(),
                request.sourceItems(),
                request.targetItems(),
                promptVersion
        );
    }

    private String buildSheetMappingPrompt(AiSuggestionRequest request) {
        return """
                Generate workbook sheet-to-file-category mapping suggestions.

                Borrower ID:
                %s

                Borrower Name:
                %s

                Workbook Name:
                %s

                Sheet Names:
                %s

                Available File Categories:
                %s

                Task:
                For each sheet name, suggest the most appropriate file category.

                Mapping hints:
                - AR, AR Dec, Accounts Receivable usually map to AR Ledger.
                - Inventory, Stock, SKU, Warehouse usually map to Inventory Report.
                - BBC, Borrowing Base, BBC Summary usually map to BBC Report.
                - Bank, Bank Statement, Recon usually map to Bank Statement.

                Important:
                - sourceItem must exactly match one value from sourceItems.
                - suggestedTargetKey must be one of targetItems.key values, or null.
                - alternatives must contain only targetItems.key values.
                - Return exactly one suggestion for each source item.

                Output JSON:
                {
                  "module": "SHEET_MAPPING",
                  "promptVersion": "%s",
                  "suggestions": [
                    {
                      "sourceItem": "AR Dec",
                      "suggestedTargetKey": "AR_LEDGER",
                      "suggestedTargetName": "AR Ledger",
                      "confidenceBand": "HIGH",
                      "reason": "The sheet appears to represent accounts receivable data.",
                      "alternatives": [],
                      "warningRequired": false,
                      "warningMessage": null
                    }
                  ]
                }
                """.formatted(
                request.borrowerId(),
                request.borrowerName(),
                request.workbookName(),
                request.sourceItems(),
                request.targetItems(),
                promptVersion
        );
    }

    public String getPromptVersion() {
        return promptVersion;
    }
}