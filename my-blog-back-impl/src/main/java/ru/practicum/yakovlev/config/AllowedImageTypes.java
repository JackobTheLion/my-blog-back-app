package ru.practicum.yakovlev.config;

import java.util.Set;

public record AllowedImageTypes(Set<String> values) {

    public AllowedImageTypes {
        values = Set.copyOf(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed image type must be configured");
        }
    }

    public boolean contains(String mediaType) {
        return values.contains(mediaType);
    }

}
