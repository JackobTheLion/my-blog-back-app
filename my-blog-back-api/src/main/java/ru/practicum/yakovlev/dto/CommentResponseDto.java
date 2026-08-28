package ru.practicum.yakovlev.dto;

public record CommentResponseDto(
        Long id,
        String text,
        Long postId
) {
}
