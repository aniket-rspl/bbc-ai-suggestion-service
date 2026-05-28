package com.jaypharma.aiservice.service.preprocessing;

import com.jaypharma.aiservice.dto.model.TargetItemDto;
import org.springframework.stereotype.Component;

@Component
public class TargetItemNormalizer {

    private final SourceItemPreprocessor sourceItemPreprocessor;

    public TargetItemNormalizer(SourceItemPreprocessor sourceItemPreprocessor) {
        this.sourceItemPreprocessor = sourceItemPreprocessor;
    }

    public NormalizedTarget normalize(TargetItemDto target) {
        String normalizedKey = sourceItemPreprocessor.preprocess(safe(target.key())).normalizedValue();
        String normalizedName = sourceItemPreprocessor.preprocess(safe(target.name())).normalizedValue();
        String normalizedDescription = sourceItemPreprocessor.preprocess(safe(target.description())).normalizedValue();
        return new NormalizedTarget(target, normalizedKey, normalizedName, normalizedDescription);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record NormalizedTarget(
            TargetItemDto original,
            String normalizedKey,
            String normalizedName,
            String normalizedDescription
    ) {
    }
}
