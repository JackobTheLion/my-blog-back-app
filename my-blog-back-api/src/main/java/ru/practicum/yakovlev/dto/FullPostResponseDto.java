package ru.practicum.yakovlev.dto;

import java.util.List;

public record FullPostResponseDto(
        Long id,
        String title,
        String text,
        List<String> tags,
        Long likesCount,
        Long commentsCount
) {
}
