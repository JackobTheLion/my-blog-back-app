package ru.practicum.yakovlev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePostRequest(
        @NotBlank(message = "Post title is required")
        String title,

        @NotBlank(message = "Post text is required")
        String text,

        @NotNull(message = "Post tags are required")
        List<@NotBlank(message = "Tag must not be blank") String> tags
) {
}
