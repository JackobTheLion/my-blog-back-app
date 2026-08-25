package ru.practicum.yakovlev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdatePostRequest(
        @NotNull(message = "Post id is required")
        @Positive(message = "Post id must be positive")
        Long id,

        @NotBlank(message = "Post title is required")
        String title,

        @NotBlank(message = "Post text is required")
        String text,

        @NotNull(message = "Post tags are required")
        List<@NotBlank(message = "Tag must not be blank") String> tags
) {
}
