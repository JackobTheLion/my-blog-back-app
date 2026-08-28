package ru.practicum.yakovlev.model;

public record ImageCleanupTask(
        Long id,
        String imagePath,
        Integer attempts,
        String lastError
) {
}
