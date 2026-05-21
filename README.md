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
