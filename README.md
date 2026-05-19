# 🧠 BBC AI Suggestion Service (V1 Prototype)

> 
> **BBC Verification Platform — Phase 2: AI Assistant & Suggestion Engine** 
> 
> 

This repository contains the V1 Prototype of the AI Suggestion Service for the Jay Pharma Collective BBC (Borrowing Base Certificate) Verification Platform.

This microservice acts as a **human-in-the-loop** AI assistant. It helps platform administrators configure borrower data by analyzing structural metadata (like column headers and Excel sheet names) and suggesting the most appropriate standard canonical fields.

**⚠️ Critical Security Notice:** This AI service operates *strictly* on metadata. **No transactional financial data** (e.g., invoice amounts, balances, customer names) is ever processed by or transmitted to the LLM. Furthermore, the AI is purely a suggestion engine; all final configurations require explicit human administrative approval.

---

## 🏗️ Architecture & Tech Stack (V1)

Prototype V1 is designed as a direct LLM-inference application without historical context (RAG) or deterministic preprocessing (which are slated for V2 and V3).

* **Core Framework:** Java 17, Spring Boot 3.x 
* **AI Integration:** Spring AI 
* **Local LLM Provider:** Ollama (for isolated, zero-cost local development) 
* **Recommended Local Model:** `llama3.2:latest` (or `gemma3:12b` / `phi4:latest` depending on hardware)
* **Response Guardrails:** Custom `SuggestionResponseSanitizer` to enforce strict JSON contracts and prevent LLM hallucinations.

---

## 🚀 Getting Started

### Prerequisites

* **Java 17** SDK installed.
* **Maven** (Included via wrapper).
* **Ollama** running locally or accessible via your organization's network.

### Setup & Configuration

1. **Clone the repository:**
```bash
git clone https://github.com/aniket-rspl/bbc-ai-suggestion-service.git
cd bbc-ai-suggestion-service

```


2. **Configure your Local Environment:**
Ensure your `src/main/resources/application-local.yml` points to your active Ollama instance.
```yaml
spring:
  ai:
    ollama:
      base-url: http://172.16.8.90:11434  # Replace with your local or org Ollama IP
      chat:
        options:
          model: llama3.2:latest
          temperature: 0  # Kept at 0 for maximum determinism in V1

```


3. **Run the Application:**
Start the service using any IDE.

The application will start on `http://localhost:8085`.

---

## 🔌 API Documentation

V1 exposes a single, polymorphic REST endpoint designed to handle multiple suggestion tasks based on the `module` parameter.

### **POST** `/api/v1/ai/suggestions`

Generates AI mapping suggestions based on provided source items and a constrained list of allowed target configurations.

#### **Request Headers**

* `Content-Type: application/json`

#### **Request Body Schema (`AiSuggestionRequest`)**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `module` | String (Enum) | **Yes** | `COLUMN_MAPPING` or `SHEET_MAPPING`. Dictates the AI's prompt behavior. |
| `borrowerId` | Long | Yes | Unique identifier for the borrower configuration. |
| `borrowerName` | String | Yes | Name of the borrowing entity context. |
| `collateralType` | String | Yes | E.g., `AR`, `Inventory`.|
| `fileCategory` | String | Yes | E.g., `AR Ledger`, `Bank Statement`.|
| `workbookName` | String | No | Needed primarily for `SHEET_MAPPING` context. |
| `sourceItems` | Array of Strings | **Yes** | The raw headers or sheet names extracted from the uploaded file.|
| `targetItems` | Array of Objects | **Yes** | The available canonical fields or categories the AI is allowed to choose from. |

**Example Request: Column Mapping**

```json
{
  "module": "COLUMN_MAPPING",
  "borrowerId": 101,
  "borrowerName": "ABC Foods",
  "collateralType": "AR",
  "fileCategory": "AR Ledger",
  "sourceItems": [
    "Trans. tp",
    "Doc no.",
    "DD"
  ],
  "targetItems": [
    {
      "key": "TRANSACTION_TYPE",
      "name": "Transaction Type",
      "dataType": "TEXT",
      "description": "Type of transaction. Examples: Invoice, General Journal, Payment."
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
      "description": "Date on which transaction should happen."
    }
  ]
}

```

#### **Response Body Schema (`AiSuggestionResponse`)**

The response is sanitized by the backend to ensure the AI only returns keys that exactly match the provided `targetItems`.

**Example Response:**

```json
{
  "module": "COLUMN_MAPPING",
  "promptVersion": "v1.0",
  "suggestions": [
    {
      "sourceItem": "Trans. tp",
      "suggestedTargetKey": "TRANSACTION_TYPE",
      "suggestedTargetName": "Transaction Type",
      "confidenceBand": "HIGH",
      "reason": "Trans. tp is a common abbreviation for Transaction Type.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "Doc no.",
      "suggestedTargetKey": "DOCUMENT_NUMBER",
      "suggestedTargetName": "Document Number",
      "confidenceBand": "HIGH",
      "reason": "Doc no. directly refers to a Document Number.",
      "alternatives": [],
      "warningRequired": false,
      "warningMessage": null
    },
    {
      "sourceItem": "DD",
      "suggestedTargetKey": "DUE_DATE",
      "suggestedTargetName": "Due Date",
      "confidenceBand": "MEDIUM",
      "reason": "DD often stands for Due Date in AR ledgers, but could also mean Direct Debit.",
      "alternatives": [],
      "warningRequired": true,
      "warningMessage": "Requires manual review due to ambiguous abbreviation."
    }
  ]
}

```

---

## 🛡️ Guardrails & Error Handling

To compensate for the unpredictable nature of pure LLM inference in V1, the application implements strict error handling and sanitization:

1. **JSON Extraction:** Strips out markdown formatting and conversational text from the Ollama response.
2. **Contract Enforcement:** If the LLM invents a target key (hallucination), the `SuggestionResponseSanitizer` drops the invalid key and sets the mapping to `null` with a `LOW` confidence warning.
3. **1-to-1 Mapping Guarantee:** Ensures the system returns exactly one suggestion object for every source item provided in the request payload.

---

## 🗺️ Roadmap

* **Phase 1 (Current):** Foundation & Direct LLM Integration.

* **Phase 2:** Implement Retrieval-Augmented Generation (RAG) using historical approved mappings.

* **Phase 3:** Introduce deterministic matching (Regex/Fuzzy/Synonyms) to bypass the LLM entirely for obvious matches.
