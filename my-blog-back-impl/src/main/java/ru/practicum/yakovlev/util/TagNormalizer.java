package ru.practicum.yakovlev.util;

import java.util.Locale;

public final class TagNormalizer {

    private TagNormalizer() {
    }

    public static String normalize(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag must not be null");
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Tag must not be blank");
        }
        return normalized;
    }
}
