package ru.practicum.yakovlev.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequestDto(
        @NotBlank(message = "Comment text is required")
        String text,
        Long postId
) {
}
