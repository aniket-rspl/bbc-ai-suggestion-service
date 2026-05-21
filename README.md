# 🧠 BBC AI Suggestion Service — V2 RAG Prototype

**BBC Verification Platform — Phase 2: AI Assistant & Suggestion Engine**

This repository contains the **V2 Prototype** of the AI Suggestion Service for the Jay Pharma Collective BBC (Borrowing Base Certificate) Verification Platform.

This microservice acts as a **human-in-the-loop AI assistant**. It helps platform administrators configure borrower data by analyzing structural metadata such as uploaded file column headers, Excel workbook sheet names, canonical field definitions, and **admin-approved historical mappings**.

The AI service suggests the most appropriate target mappings, but it does **not** make final decisions. All mappings must be reviewed, accepted, corrected, or rejected by an administrator.

---

## ⚠️ Critical Security Notice

This AI service operates strictly on **metadata**.

The service must not process or transmit borrower transactional financial data to the LLM, including but not limited to:

* Invoice amounts
* Customer names from transaction rows
* Balances
* Payments
* Transaction-level financial records

Only structural metadata is used, such as header names, sheet names, canonical field names/descriptions, file category names, and historical admin-approved mappings. The AI is purely a **suggestion engine**. The administrator’s decision is always final.

---

## 🏗️ Architecture & Tech Stack

V2 extends the V1 direct LLM prototype by adding **Retrieval-Augmented Generation (RAG)** using historical admin-approved mappings.

### Core Stack

* Java 17
* Spring Boot 4.x
* Spring AI
* Maven Wrapper

### Local AI & Vector Components

* **Local LLM Execution:** Ollama
* **Vector Generation:** Ollama Embeddings
* **Vector Database:** Spring AI `VectorStore` (`SimpleVectorStore` for local prototype/demo storage)

### Recommended Local Models

* **Chat Model:** `llama3.2:latest` (Alternatives: `gemma3:12b`, `phi4:latest`)
* **Embedding Model:** `mxbai-embed-large:latest` (Alternative: `bge-m3:latest`)

---

## 🧭 V2 High-Level Flow

### 1. Suggestion Flow

1. Admin uploads file metadata.
2. System extracts headers / sheet names.
3. Suggestion API receives `sourceItems` + `targetItems`.
4. **Per-source-item vector retrieval** fetches compact approved historical mapping hints.
5. Single LLM call generates sanitized structured suggestions.
6. Admin reviews, accepts, or corrects the mappings.

### 2. Learning Flow

1. Admin accepts or corrects suggestions.
2. Frontend sends **one batch learning request**.
3. Backend stores each approved/corrected mapping as a separate vector document in the `VectorStore`.
4. Future RAG suggestions improve based on this learned context.

> **Note:** The learning endpoint is intentionally batch-based. The UI should not call the backend once per suggestion row. Instead, after the admin completes the review, all accepted and corrected mappings are sent in one single request.

---

## ✅ What V2 Adds Over V1

**V1 Flow:** `Request -> Direct LLM -> JSON Response -> Sanitizer`

**V2 Flow:** `Request -> Per-source-item RAG retrieval -> Compact historical mapping hints -> Direct LLM -> JSON Response -> Sanitizer -> Admin Review -> Batch Learning API -> Vector Store`

**Key V2 Improvements:**

* Learns from admin-approved and admin-corrected mappings.
* Performs per-source-item retrieval instead of one global retrieval to ensure no mappings are missed.
* Stores each approved mapping as an individual vector document.
* Uses compact RAG hints to avoid large prompts, keeping operations fast and cost-effective.
* Maintains one LLM call per suggestion request and one learning API call per admin review batch.

---

## 🚀 Getting Started

### Prerequisites

* Java 17 SDK installed.
* Ollama running locally or accessible through the organization network.
* Required Ollama chat and embedding models pulled (`ollama pull gemma3:4b` and `ollama pull mxbai-embed-large:latest`).

### ⚙️ Local Configuration

Update your `src/main/resources/application-local.yml` to include the vector store and embedding configurations:

```yaml
server:
  port: 8085

spring:
  application:
    name: bbc-ai-suggestion-service
  ai:
    ollama:
      base-url: http://172.16.8.90:11434
      chat:
        options:
          model: gemma3:4b
          temperature: 0
      embedding:
        options:
          model: mxbai-embed-large:latest
    model:
      embedding: ollama

```
### ▶️ Run the Application

The service will start on `http://localhost:8085`.

---

## 🔌 API Documentation

### 1. Generate AI Suggestions

`POST /api/v1/ai/suggestions`

Generates AI mapping suggestions based on source items, allowed target items, and historical RAG context.

**Request Headers:** `Content-Type: application/json`

**Request Body Schema (`AiSuggestionRequest`)**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `module` | String (Enum) | Yes | `COLUMN_MAPPING` or `SHEET_MAPPING` |
| `borrowerId` | Long | No | Borrower identifier |
| `borrowerName` | String | No | Borrower name context |
| `collateralType` | String | No | Example: `AR`, `Inventory` |
| `fileCategory` | String | No | Example: `AR Ledger`, `Bank Statement` |
| `workbookName` | String | No | Uploaded workbook name |
| `sourceItems` | Array of Strings | Yes | Headers or sheet names extracted from uploaded file |
| `targetItems` | Array of Objects | Yes | Canonical fields/categories the AI is allowed to choose from |

### 2. Batch Learning API

`POST /api/v1/ai/learning/approved-mappings`

Stores admin-approved and admin-corrected mappings for future RAG retrieval. **Only send mappings that the admin has accepted or corrected.** Do not send rejected or null mappings (e.g., `Remarks -> null`).

**Request Body Schema (`AiLearningRequest`)**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `module` | String (Enum) | Yes | Suggestion module (`COLUMN_MAPPING` or `SHEET_MAPPING`) |
| `borrowerId` | Long | No | Borrower identifier |
| `promptVersion` | String | No | Prompt version returned by the suggestion API |
| `approvedBy` | String | No | Admin/user who approved the mappings |
| `suggestions` | Array of Objects | Yes | The accepted/corrected mappings |

---


### 1. AI Suggestion Payload

**AI Suggestion Request: Column Mapping**

```json
{
  "module": "COLUMN_MAPPING",
  "borrowerId": 101,
  "borrowerName": "ABC Foods",
  "collateralType": "AR",
  "fileCategory": "AR Ledger",
  "workbookName": "abc-foods-ar-ledger.xlsx",
  "sourceItems": [
    "Trans. tp",
    "Doc no.",
    "DD",
    "Op bal",
    "credits"
  ],
  "targetItems": [
    {
      "key": "TRANSACTION_TYPE",
      "name": "Transaction Type",
      "dataType": "TEXT",
      "description": "Type of transaction. Examples: Invoice, General Journal, Payment, Credit Memo."
    },
    {
      "key": "DOCUMENT_NUMBER",
      "name": "Document Number",
      "dataType": "TEXT",
      "description": "Unique document identifier."
    },
    {
      "key": "DUE_DATE",
      "name": "Due Date",
      "dataType": "DATE",
      "description": "Date on which payment is due."
    },
    {
      "key": "OPENING_AMOUNT",
      "name": "Opening Amount",
      "dataType": "DECIMAL",
      "description": "Opening amount or opening balance."
    },
    {
      "key": "CREDIT_AMOUNT",
      "name": "Credit Amount",
      "dataType": "DECIMAL",
      "description": "Credit amount or credit memo amount."
    }
  ]
}

```

**AI Suggestion Response: Column Mapping**

```json
{
  "module": "COLUMN_MAPPING",
  "promptVersion": "v2-rag-001",
  "suggestions": [
    {
      "sourceItem": "Trans. tp",
      "suggestedTargetKey": "TRANSACTION_TYPE",
      "suggestedTargetName": "Transaction Type",
      "confidenceBand": "HIGH",
      "reason": "Trans. tp is a common abbreviation for transaction type.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Doc no.",
      "suggestedTargetKey": "DOCUMENT_NUMBER",
      "suggestedTargetName": "Document Number",
      "confidenceBand": "HIGH",
      "reason": "Doc no. refers to document number.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "DD",
      "suggestedTargetKey": "DUE_DATE",
      "suggestedTargetName": "Due Date",
      "confidenceBand": "MEDIUM",
      "reason": "DD may represent due date in AR context, but it can be ambiguous.",
      "alternatives": [],
      "warningRequired": true,
      "warningMessage": "Manual review required because DD is ambiguous."
    }
  ]
}

```

**AI Suggestion Request: Sheet Mapping**

```json
{
  "module": "SHEET_MAPPING",
  "borrowerId": 101,
  "borrowerName": "ABC Foods",
  "collateralType": "AR",
  "workbookName": "abc-foods-month-end-pack.xlsx",
  "sourceItems": [
    "AR Aging Jan",
    "Stock Summary",
    "Borrowing Base Cert",
    "Bank Reconciliation",
    "Random Notes"
  ],
  "targetItems": [
    {
      "key": "AR_LEDGER",
      "name": "AR Ledger",
      "dataType": "FILE",
      "description": "Accounts Receivable Ledger or Aging Report detailing outstanding invoices."
    },
    {
      "key": "INVENTORY_REPORT",
      "name": "Inventory Report",
      "dataType": "FILE",
      "description": "Stock summary or inventory valuation report."
    },
    {
      "key": "BBC_REPORT",
      "name": "BBC Report",
      "dataType": "FILE",
      "description": "Borrowing Base Certificate detailing eligible collateral."
    },
    {
      "key": "BANK_STATEMENT",
      "name": "Bank Statement",
      "dataType": "FILE",
      "description": "Monthly bank statements or reconciliation files."
    }
  ]
}

```

**AI Suggestion Response: Sheet Mapping**

```json
{
  "module": "SHEET_MAPPING",
  "promptVersion": "v2-rag-001",
  "suggestions": [
    {
      "sourceItem": "AR Aging Jan",
      "suggestedTargetKey": "AR_LEDGER",
      "suggestedTargetName": "AR Ledger",
      "confidenceBand": "HIGH",
      "reason": "AR Aging is a standard term for an Accounts Receivable Ledger.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Stock Summary",
      "suggestedTargetKey": "INVENTORY_REPORT",
      "suggestedTargetName": "Inventory Report",
      "confidenceBand": "HIGH",
      "reason": "Stock Summary directly relates to inventory reporting.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Borrowing Base Cert",
      "suggestedTargetKey": "BBC_REPORT",
      "suggestedTargetName": "BBC Report",
      "confidenceBand": "HIGH",
      "reason": "Exact match for Borrowing Base Certificate acronym (BBC).",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Bank Reconciliation",
      "suggestedTargetKey": "BANK_STATEMENT",
      "suggestedTargetName": "Bank Statement",
      "confidenceBand": "HIGH",
      "reason": "Bank Reconciliations are typically derived from or act as Bank Statements.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Random Notes",
      "suggestedTargetKey": null,
      "suggestedTargetName": null,
      "confidenceBand": "LOW",
      "reason": "Random Notes does not map to any standard financial collateral file category.",
      "alternatives": [],
      "warningRequired": true,
      "warningMessage": "No suitable mapping found. Manual review required."
    }
  ]
}

```

---

### 2. AI Learning Payload 

**Learning Request**

```json
{
  "module": "COLUMN_MAPPING",
  "borrowerId": 304,
  "borrowerName": "Evergreen Distribution",
  "collateralType": "AR",
  "fileCategory": "AR Ledger",
  "workbookName": "evergreen-ar-open-items.xlsx",
  "promptVersion": "v2-rag-001",
  "approvedBy": "local-test-admin",
  "suggestions": [
    {
      "sourceItem": "Transaction Code",
      "originalSuggestedTargetKey": "TRANSACTION_TYPE",
      "originalSuggestedTargetName": "Transaction Type",
      "acceptedTargetKey": "TRANSACTION_TYPE",
      "acceptedTargetName": "Transaction Type",
      "decisionType": "ACCEPTED",
      "confidenceBand": "LOW",
      "reason": "Admin confirmed Transaction Code represents transaction type in this AR ledger.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Reference No",
      "originalSuggestedTargetKey": "DOCUMENT_NUMBER",
      "originalSuggestedTargetName": "Document Number",
      "acceptedTargetKey": "DOCUMENT_NUMBER",
      "acceptedTargetName": "Document Number",
      "decisionType": "ACCEPTED",
      "confidenceBand": "MEDIUM",
      "reason": "Admin confirmed Reference No is used as the document number.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Credit",
      "originalSuggestedTargetKey": "CREDIT_AMOUNT",
      "originalSuggestedTargetName": "Credit Amount",
      "acceptedTargetKey": "CREDIT_AMOUNT",
      "acceptedTargetName": "Credit Amount",
      "decisionType": "ACCEPTED",
      "confidenceBand": "HIGH",
      "reason": "Admin confirmed Credit represents credit amount.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    }
  ]
}

```

**Learning Response**

```json
{
  "status": "INGESTED",
  "requestedCount": 3,
  "ingestedCount": 3,
  "learningIds": [
    "7e29b7e4-b13a-4b35-94fb-4af3ce4456a1",
    "cbda5d4f-40c1-49fd-a99f-507e476b9a3d",
    "a7b4f52a-64e7-4063-b622-e7aa9927fcde"
  ]
}

```
---

## 🧠 RAG Strategy

### Why Per-Source-Item Retrieval?

A single global vector search (e.g., *all sourceItems + all targetItems -> top 8 historical mappings*) is unreliable. If a request has 20 headers, an important mapping might be missed. V2 solves this by performing a vector similarity search for **each individual source item**, retrieving the top 1–2 historical hints specifically for that item.

### Compact RAG Context

Instead of injecting full historical JSON documents into the LLM prompt, the system injects highly compact mapping hints. This keeps the prompt smaller, cheaper, and easier for the LLM to follow:

> *Approved historical mapping hints:*
> *- Current source item "Transaction Code" is similar to learned "Trx Cd" -> TRANSACTION_TYPE.*
> *- Current source item "Reference No" is similar to learned "Ref Num" -> DOCUMENT_NUMBER.*

---

## 🛡️ Guardrails & Error Handling

To compensate for LLM variability, the application enforces strict contracts via the `SuggestionResponseSanitizer`:

* **JSON Extraction:** Automatically strips markdown formatting (e.g., ```json) to extract raw JSON objects.
* **1-to-1 Mapping Guarantee:** Ensures the response contains exactly one suggestion per source item.
* **Hallucination Prevention:** If the LLM invents a target key not present in `targetItems`, the sanitizer drops the key, downgrades the confidence to `LOW`, and flags it for manual review.

---

## 🗺️ Roadmap

**Phase 1 — Completed**

* Direct LLM suggestion endpoint.
* Generic `COLUMN_MAPPING` and `SHEET_MAPPING` support.
* Strict JSON sanitization and local Ollama integration.

**Phase 2 — Current**

* **RAG-based improvement** using admin-approved mappings.
* Batch learning endpoint and vector store ingestion.
* Per-source-item retrieval with compact RAG hints.

**Phase 3 — Next (V3)**

* **Deterministic preprocessing and matching.**
* Header normalization, abbreviation expansion, and synonym dictionaries.
* **Skip LLM for high-confidence deterministic matches** to save compute time, sending only unresolved or ambiguous items to the RAG + LLM engine.

```

```
