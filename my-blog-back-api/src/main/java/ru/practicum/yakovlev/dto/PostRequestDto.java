package ru.practicum.yakovlev.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostRequestDto(
        Long id,
        @NotBlank(message = "Post title is required")
        String title,
        @NotBlank(message = "Post text is required")
        String text,
        List<@NotBlank(message = "Tag must not be blank") String> tags
) {
}
