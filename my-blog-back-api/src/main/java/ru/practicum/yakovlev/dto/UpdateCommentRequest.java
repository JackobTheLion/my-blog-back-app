package ru.practicum.yakovlev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCommentRequest(
        @NotNull(message = "Comment id is required")
        @Positive(message = "Comment id must be positive")
        Long id,

        @NotBlank(message = "Comment text is required")
        String text,

        @NotNull(message = "Post id is required")
        @Positive(message = "Post id must be positive")
        Long postId
) {
}
