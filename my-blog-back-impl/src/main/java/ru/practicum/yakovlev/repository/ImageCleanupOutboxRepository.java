package ru.practicum.yakovlev.repository;

import ru.practicum.yakovlev.model.ImageCleanupTask;

import java.util.List;

public interface ImageCleanupOutboxRepository {

    void enqueueDelete(String imagePath);

    List<ImageCleanupTask> findPending(int limit);

    void deleteById(Long id);

    void recordFailure(Long id, String error);
}
