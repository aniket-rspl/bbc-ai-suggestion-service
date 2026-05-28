package com.jaypharma.aiservice.dto.model;

import com.jaypharma.aiservice.dto.module.DetectedFieldType;

import java.util.List;

public record NormalizedSourceItem(
        String originalValue,
        String cleanedValue,
        String normalizedValue,
        DetectedFieldType detectedFieldType,
        boolean ambiguous,
        List<String> ambiguityReasons
) {
}
