## 🧠 BBC AI Suggestion Service - V3 Deterministic + RAG + LLM (Prototype)

**BBC Verification Platform - Phase 2: AI Assistant and Suggestion Engine**

This repository contains the **V3 prototype** of the AI Suggestion Service for the Jay Pharma Collective BBC (Borrowing Base Certificate) Verification Platform.

This microservice acts as a **human-in-the-loop suggestion engine**. It helps platform administrators configure borrower metadata by analyzing uploaded file column headers, Excel workbook sheet names, canonical target fields/categories, and **admin decisions + historical approved mappings**.

The AI service suggests the most appropriate target mappings, but it does **not** make final decisions. Admin decisions are always final.

---

## ⚠️ Critical Security Notice

This AI service operates strictly on **structural metadata**.

The service must not process or transmit borrower transactional financial data to the LLM, including:

- Invoice amounts
- Customer names from transaction rows
- Balances
- Payments
- Transaction-level financial records

Only metadata is used (headers, sheet names, canonical field names/descriptions, file category names, and historical admin-approved mappings).

---

## 🏗️ Architecture and Tech Stack

### Core Stack

- Java 17
- Spring Boot 4.x
- Spring AI
- Maven Wrapper

### Local AI and Vector Components

- **Local LLM Execution:** Ollama
- **Vector Generation:** Ollama embeddings
- **Vector Store (prototype):** Spring AI `VectorStore` using `SimpleVectorStore` (in-memory)

### Normalization and Deterministic Matching (V3)

V3 adds **deterministic preprocessing and matching** before using any tokens:

- **Normalization config (seed):** `src/main/resources/ai-normalization.yml`
- **Runtime alias overlay:** in-memory `NormalizationKnowledgeStore` updates based on admin decisions

---

## 🧪 Prototype Notice (In-Memory Stores)

This is a **prototype**.

- **Normalization aliases** are seeded from `src/main/resources/ai-normalization.yml` and updated at runtime in an **in-memory overlay** (`NormalizationKnowledgeStore`).
- **Vector store** uses Spring AI `SimpleVectorStore` (in-memory).

If the app restarts, both the in-memory alias overlay and the vector store contents are lost.

### Production expectation

For production environments you should:

- Replace the in-memory normalization store with a **DB backed store** (so learned aliases persist).
- Replace `SimpleVectorStore` with a **persistent vector store** (Azure AI Search or another production-grade vector DB).
- Add audit tables for learning decisions if required by compliance.

---

## 🧭 V3 High-Level Flow

### 1. Suggestion Flow

1. Admin uploads a file (main app extracts headers or sheet names).
2. Suggestion API receives `sourceItems` + allowed `targetItems`.
3. **Preprocessing** normalizes each source item (aliases, abbreviations, ambiguity flags).
4. **Deterministic matching** attempts HIGH-confidence matches using normalized text and configured aliases.
5. Only unresolved/ambiguous items go to **RAG** (per-source-item retrieval).
6. Only unresolved/ambiguous items go to **LLM** (single LLM call for the reduced list).
7. Merge deterministic + LLM results, then sanitize and return one suggestion per source item.
8. Admin reviews and accepts or corrects mappings.

### 2. Unified Learning Flow (single API)

1. Admin completes review (accepted or corrected rows only).
2. Frontend sends **one batch learning request**.
3. Backend updates both, in one operation:
   - **Alias store** (deterministic learning)
   - **Vector store** (RAG learning)
4. Future suggestions improve:
   - more deterministic HIGH matches
   - fewer RAG/LLM calls

---

## ✅ What V3 Adds Over V2

**V2 Flow:** Request -> per-source-item RAG -> LLM -> sanitizer -> admin review -> vector store learning

**V3 Flow:** Request -> deterministic preprocessing/matching -> RAG (unresolved) -> LLM (unresolved) -> sanitizer -> admin review -> **unified learning (aliases + vector store)**

Key V3 improvements:

- Deterministic HIGH-confidence matches skip AI calls (token savings).
- LLM is called only for unresolved items.
- Learning is unified: one API updates both alias knowledge and vector knowledge.

---

## 🚀 Getting Started

### Prerequisites

- Java 17 SDK installed
- Ollama running locally or accessible through your network
- Required models pulled (example):

```bash
ollama pull gemma3:4b
ollama pull mxbai-embed-large:latest
```

### Configuration

Main config lives in `src/main/resources/application.yml` and imports `ai-normalization.yml`:

- Ollama base URL: `spring.ai.ollama.base-url`
- Chat model: `spring.ai.ollama.chat.options.model`
- Embedding model: `spring.ai.ollama.embedding.options.model`
- Prompt version: `bbc.ai.prompt-version`
- Normalization config: `src/main/resources/ai-normalization.yml`

### Run the Application

The service starts on `http://localhost:8085`.

---

## API Documentation

### 1. Generate AI Suggestions

`POST /api/v1/ai/suggestions`

Generates mapping suggestions based on `sourceItems`, allowed `targetItems`, and historical RAG context.

**Request Body (`AiSuggestionRequest`)**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `module` | String (Enum) | Yes | `COLUMN_MAPPING` or `SHEET_MAPPING` |
| `borrowerId` | Long | No | Borrower identifier |
| `borrowerName` | String | No | Borrower name context |
| `collateralType` | String | No | Example: `AR`, `Inventory` |
| `fileCategory` | String | No | Example: `AR Ledger`, `Bank Statement` |
| `workbookName` | String | No | Workbook name |
| `sourceItems` | Array[String] | Yes | Headers or sheet names |
| `targetItems` | Array[Object] | Yes | Allowed canonical targets (keys) |

### 2. Unified Batch Learning API (aliases + vector store)

`POST /api/v1/ai/learning/mapping-decisions`

Stores admin **accepted/corrected** decisions and learns from them for both:

- deterministic alias matching
- vector store RAG retrieval

Compatibility notes:

- `suggestions` is accepted as an alias for `decisions`
- `acceptedTargetKey/acceptedTargetName` are accepted as aliases for `finalTargetKey/finalTargetName`
- `approvedBy` is accepted as an alias for `decidedBy`

**Request Body (`AiLearningRequest`)**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `module` | String (Enum) | Yes | `COLUMN_MAPPING` or `SHEET_MAPPING` |
| `borrowerId` | Long | No | Borrower identifier |
| `promptVersion` | String | No | Prompt version returned by suggestion API |
| `decidedBy` | String | No | Admin/user who decided |
| `decisions` | Array[Object] | Yes | Accepted/corrected rows only |

Deprecated endpoint (kept for backward compatibility):

- `POST /api/v1/ai/learning/approved-mappings` -> same behavior as `mapping-decisions`

---

## 🧠 Strategy: Deterministic + RAG + LLM (Detailed)

### 1) Deterministic matching

Used first for every `sourceItem`.

- **Input**: a source header or sheet name, plus the allowed `targetItems`
- **Normalization**: token cleanup, alias expansion, ambiguity flags from `ai-normalization.yml`
- **Strategies** (in order):
  - exact match
  - normalized exact match
  - synonym match (from alias knowledge store)
- **Outcome**:
  - **HIGH** confidence results are returned immediately without calling RAG or LLM.
  - ambiguous inputs stay unresolved (example: `CR`, `DD`, `Invt`).

### 2) RAG (per-source-item retrieval)

Used only for unresolved items.

- For each unresolved `sourceItem`, the service queries the vector store for the top similar **approved mappings**.
- The prompt uses **compact hints** instead of dumping full historical documents.

### 3) LLM (only for unresolved items)

Used only after deterministic matching (and with RAG hints when available).

- The service makes **one** LLM call for the reduced list of unresolved items.
- Output must be valid JSON and must choose only from allowed `targetItems`.
- Final response is sanitized to prevent hallucinated keys and to guarantee one suggestion per source item.

---

## 🛡️ Guardrails and Error Handling

To compensate for LLM variability, the service enforces strict contracts:

- **JSON extraction**: fenced responses like ```json are stripped and parsed.
- **1 to 1 mapping guarantee**: final response contains exactly one suggestion per `sourceItem`.
- **Hallucination prevention**: if the LLM returns a target key not present in `targetItems`, the sanitizer drops the key, downgrades confidence to `LOW`, and flags manual review.
- **Manual review flags**: low confidence or null target results are returned with warnings.

---

## Example Payloads

### 1. Suggestion Request (Column Mapping)

```json
{
  "module": "COLUMN_MAPPING",
  "borrowerId": 712,
  "borrowerName": "Harborline Wholesale",
  "collateralType": "AR",
  "fileCategory": "AR Open Items",
  "workbookName": "harborline-ar-may-2026.xlsx",
  "sourceItems": [
    "Ref Num",
    "Inv Dt",
    "Pmt Due Dt",
    "Op Bal",
    "Bal Remaining",
    "CR",
    "DD"
  ],
  "targetItems": [
    { "key": "DOCUMENT_NUMBER", "name": "Document Number", "dataType": "TEXT", "description": "Unique reference number." },
    { "key": "INVOICE_DATE", "name": "Invoice Date", "dataType": "DATE", "description": "Date the invoice was issued." },
    { "key": "DUE_DATE", "name": "Due Date", "dataType": "DATE", "description": "Payment due date." },
    { "key": "OPENING_AMOUNT", "name": "Opening Amount", "dataType": "DECIMAL", "description": "Opening balance." },
    { "key": "OUTSTANDING_AMOUNT", "name": "Outstanding Amount", "dataType": "DECIMAL", "description": "Unpaid balance remaining." },
    { "key": "CREDIT_AMOUNT", "name": "Credit Amount", "dataType": "DECIMAL", "description": "Credit or payment amount." }
  ]
}
```

### 1.1 Suggestion Response (Column Mapping, sample)

This is an example shape. Your actual response depends on current aliases, RAG history, and LLM output.

```json
{
  "module": "COLUMN_MAPPING",
  "promptVersion": "v3-deterministic-001",
  "suggestions": [
    {
      "sourceItem": "Ref Num",
      "suggestedTargetKey": "DOCUMENT_NUMBER",
      "suggestedTargetName": "Document Number",
      "confidenceBand": "HIGH",
      "reason": "Matched by deterministic NORMALIZED_EXACT: Document Number.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "CR",
      "suggestedTargetKey": "CREDIT_AMOUNT",
      "suggestedTargetName": "Credit Amount",
      "confidenceBand": "MEDIUM",
      "reason": "CR is ambiguous. Using best guess with manual review.",
      "alternatives": [],
      "warningRequired": true,
      "warningMessage": "Manual review required because CR can mean multiple things."
    }
  ]
}
```

### 2. Unified Learning Request (Accepted + Corrected)

Only send rows the admin mapped (accepted or corrected). Do not send null/unmapped rows in this V3 prototype.

```json
{
  "module": "COLUMN_MAPPING",
  "borrowerId": 712,
  "borrowerName": "Harborline Wholesale",
  "collateralType": "AR",
  "fileCategory": "AR Open Items",
  "workbookName": "harborline-ar-may-2026.xlsx",
  "promptVersion": "v3-deterministic-001",
  "decidedBy": "local-test-admin",
  "decisions": [
    {
      "sourceItem": "Ref Num",
      "decisionType": "ACCEPTED",
      "suggestionSource": "DETERMINISTIC",
      "originalSuggestedTargetKey": "DOCUMENT_NUMBER",
      "originalSuggestedTargetName": "Document Number",
      "finalTargetKey": "DOCUMENT_NUMBER",
      "finalTargetName": "Document Number",
      "confidenceBand": "HIGH",
      "reason": "Accepted mapping."
    },
    {
      "sourceItem": "Pmt Due Dt",
      "decisionType": "CORRECTED",
      "suggestionSource": "LLM",
      "originalSuggestedTargetKey": "INVOICE_DATE",
      "originalSuggestedTargetName": "Invoice Date",
      "finalTargetKey": "DUE_DATE",
      "finalTargetName": "Due Date",
      "confidenceBand": "HIGH",
      "reason": "Corrected: payment due date, not invoice date."
    }
  ]
}
```

### 2.1 Unified Learning Response (sample)

```json
{
  "status": "COMPLETED",
  "module": "COLUMN_MAPPING",
  "requestedCount": 2,
  "learnedCount": 2,
  "aliasUpdatedCount": 2,
  "vectorIngestedCount": 2,
  "failedCount": 0,
  "learningIds": [
    "94f3669b-c6b3-4ead-8e6c-982b6df1d897",
    "5af1b318-0d92-4e74-8b9b-3e1a2c1d0b90"
  ],
  "items": [
    {
      "sourceItem": "Ref Num",
      "decisionType": "ACCEPTED",
      "status": "LEARNED",
      "vectorLearningId": "94f3669b-c6b3-4ead-8e6c-982b6df1d897",
      "aliasUpdated": true,
      "canonicalPhrase": "document number",
      "aliasesAdded": ["Ref Num"],
      "errorMessage": null
    }
  ]
}
```

### 3. Suggestion Request (Sheet Mapping)

```json
{
  "module": "SHEET_MAPPING",
  "borrowerId": 712,
  "borrowerName": "Harborline Wholesale",
  "collateralType": "AR",
  "workbookName": "harborline-month-end-pack.xlsx",
  "sourceItems": [
    "AR Aging May",
    "Stock Summary",
    "Borrowing Base Cert",
    "Bank Recon",
    "Random Notes"
  ],
  "targetItems": [
    { "key": "AR_LEDGER", "name": "AR Ledger", "dataType": "FILE", "description": "Accounts receivable ledger or aging report." },
    { "key": "INVENTORY_REPORT", "name": "Inventory Report", "dataType": "FILE", "description": "Stock summary or inventory valuation report." },
    { "key": "BBC_REPORT", "name": "BBC Report", "dataType": "FILE", "description": "Borrowing base certificate report." },
    { "key": "BANK_STATEMENT", "name": "Bank Statement", "dataType": "FILE", "description": "Monthly bank statement or reconciliation." }
  ]
}
```

### 3.1 Suggestion Response (Sheet Mapping, sample)

```json
{
  "module": "SHEET_MAPPING",
  "promptVersion": "v3-deterministic-001",
  "suggestions": [
    {
      "sourceItem": "AR Aging May",
      "suggestedTargetKey": "AR_LEDGER",
      "suggestedTargetName": "AR Ledger",
      "confidenceBand": "HIGH",
      "reason": "Matched by deterministic SYNONYM: ar ledger.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Random Notes",
      "suggestedTargetKey": null,
      "suggestedTargetName": null,
      "confidenceBand": "LOW",
      "reason": "No direct mapping available.",
      "alternatives": [],
      "warningRequired": true,
      "warningMessage": "No suitable mapping found. Manual review required."
    }
  ]
}
```

### 4. Unified Learning Request (Sheet Mapping)

```json
{
  "module": "SHEET_MAPPING",
  "borrowerId": 712,
  "borrowerName": "Harborline Wholesale",
  "collateralType": "AR",
  "workbookName": "harborline-month-end-pack.xlsx",
  "promptVersion": "v3-deterministic-001",
  "decidedBy": "local-test-admin",
  "decisions": [
    {
      "sourceItem": "Bank Recon",
      "decisionType": "CORRECTED",
      "suggestionSource": "LLM",
      "originalSuggestedTargetKey": "BANK_STATEMENT",
      "originalSuggestedTargetName": "Bank Statement",
      "finalTargetKey": "BANK_STATEMENT",
      "finalTargetName": "Bank Statement",
      "confidenceBand": "MEDIUM",
      "reason": "Admin confirmed Bank Recon means bank statement or reconciliation pack."
    }
  ]
}
```

