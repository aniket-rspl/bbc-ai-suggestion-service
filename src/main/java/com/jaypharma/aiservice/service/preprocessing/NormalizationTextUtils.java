package com.jaypharma.aiservice.service.preprocessing;

import java.util.Locale;

public final class NormalizationTextUtils {

    private NormalizationTextUtils() {
    }

    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
