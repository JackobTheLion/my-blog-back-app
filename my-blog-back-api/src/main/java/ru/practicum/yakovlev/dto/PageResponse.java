package ru.practicum.yakovlev.dto;

import java.util.List;

public record PageResponse(
        List<FullPostResponseDto> posts,
        Boolean hasPrev,
        Boolean hasNext,
        Long lastPage
) {
}
