package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.model.ImageCleanupTask;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcImageCleanupOutboxRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Test
    @DisplayName("Enqueues only one deletion task for the same image path")
    void shouldEnqueueOnlyUniqueImagePaths() {
        imageCleanupOutboxRepository.enqueueDelete("1/image.png");
        imageCleanupOutboxRepository.enqueueDelete("1/image.png");

        List<ImageCleanupTask> tasks = imageCleanupOutboxRepository.findPending(10);

        assertEquals(1, tasks.size());
        assertEquals("1/image.png", tasks.getFirst().imagePath());
        assertEquals(0, tasks.getFirst().attempts());
        assertNull(tasks.getFirst().lastError());
    }

    @Test
    @DisplayName("Returns pending tasks in insertion order and respects the limit")
    void shouldFindPendingTasksInInsertionOrderWithinLimit() {
        imageCleanupOutboxRepository.enqueueDelete("1/first.png");
        imageCleanupOutboxRepository.enqueueDelete("1/second.png");
        imageCleanupOutboxRepository.enqueueDelete("1/third.png");

        List<String> paths = imageCleanupOutboxRepository.findPending(2).stream()
                .map(ImageCleanupTask::imagePath)
                .toList();

        assertEquals(List.of("1/first.png", "1/second.png"), paths);
    }

    @Test
    @DisplayName("Records a failure and postpones the next attempt")
    void shouldRecordFailureAndPostponeRetry() {
        imageCleanupOutboxRepository.enqueueDelete("1/image.png");
        Long taskId = imageCleanupOutboxRepository.findPending(10).getFirst().id();

        imageCleanupOutboxRepository.recordFailure(taskId, "disk unavailable");

        Map<String, Object> task = jdbcTemplate.queryForMap("""
                SELECT attempts, last_error
                FROM image_cleanup_outbox
                WHERE id = :id
                """, Map.of("id", taskId));
        assertEquals(1, task.get("attempts"));
        assertEquals("disk unavailable", task.get("last_error"));
        assertTrue(imageCleanupOutboxRepository.findPending(10).isEmpty());
    }

    @Test
    @DisplayName("Deletes a completed task")
    void shouldDeleteCompletedTask() {
        imageCleanupOutboxRepository.enqueueDelete("1/image.png");
        Long taskId = imageCleanupOutboxRepository.findPending(10).getFirst().id();

        imageCleanupOutboxRepository.deleteById(taskId);

        assertTrue(imageCleanupOutboxRepository.findPending(10).isEmpty());
    }
}
