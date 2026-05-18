package com.jaypharma.aiservice.service;

import com.jaypharma.aiservice.dto.AiSuggestionRequest;
import com.jaypharma.aiservice.dto.AiSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectLlmSuggestionService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final SuggestionPromptBuilder promptBuilder;
    private final SuggestionResponseSanitizer sanitizer;

    public AiSuggestionResponse suggest(AiSuggestionRequest request) {
        ChatClient chatClient = chatClientBuilder.build();

        String prompt = promptBuilder.build(request);

        log.info(
                "Calling LLM. module={}, borrowerId={}, sourceItemCount={}, targetItemCount={}",
                request.module(),
                request.borrowerId(),
                request.sourceItems().size(),
                request.targetItems().size()
        );

        String rawResponse = chatClient.prompt()
                .system("""
                        You are an AI assistant for the BBC Verification Platform.

                        Critical rules:
                        1. Return raw JSON only.
                        2. Do not wrap the response in markdown.
                        3. Do not use ```json code fences.
                        4. Do not include explanations outside the JSON.
                        5. You are not a decision-maker.
                        6. You only provide suggestions.
                        7. The admin user's decision is always final.
                        8. Do not invent source items.
                        9. Do not invent target items.
                        10. Return exactly one suggestion for each source item.
                        11. suggestedTargetKey must be either null or one of the provided target item keys.
                        12. alternatives must contain only target item keys.
                        13. If no suitable mapping exists, return null target key and LOW confidence.
                        14. Do not request or use borrower transactional financial data.
                        """)
                .user(prompt)
                .call()
                .content();

        log.debug("Raw LLM response. module={}, borrowerId={}, response={}",
                request.module(), request.borrowerId(), rawResponse);

        AiSuggestionResponse response = parseResponse(rawResponse);
        return sanitizer.sanitize(request, response);
    }

    private AiSuggestionResponse parseResponse(String rawResponse) {
        try {
            String cleanedResponse = extractJson(rawResponse);
            return objectMapper.readValue(cleanedResponse, AiSuggestionResponse.class);
        } catch (Exception ex) {
            log.error("Failed to parse LLM response. rawResponse={}", rawResponse, ex);
            throw new IllegalStateException("LLM returned invalid response format");
        }
    }

    private String extractJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("LLM returned empty response");
        }

        String response = rawResponse.trim();

        if (response.startsWith("```json")) {
            response = response.substring("```json".length()).trim();
        } else if (response.startsWith("```")) {
            response = response.substring("```".length()).trim();
        }

        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - "```".length()).trim();
        }

        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');

        if (firstBrace < 0 || lastBrace < 0 || lastBrace <= firstBrace) {
            throw new IllegalStateException("No valid JSON object found in LLM response");
        }

        return response.substring(firstBrace, lastBrace + 1).trim();
    }
}