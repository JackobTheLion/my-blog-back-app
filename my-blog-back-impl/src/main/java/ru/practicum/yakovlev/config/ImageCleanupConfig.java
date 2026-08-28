package ru.practicum.yakovlev.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.practicum.yakovlev.model.ImageCleanupTask;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;
import ru.practicum.yakovlev.storage.ImageStorage;

import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
public class ImageCleanupConfig {

    private final ImageCleanupOutboxRepository outboxRepository;
    private final ImageStorage imageStorage;
    private final int batchSize;
    private final int maxBatchesPerRun;

    public ImageCleanupConfig(
            ImageCleanupOutboxRepository outboxRepository,
            ImageStorage imageStorage,
            @Value("${blog.image-storage.cleanup.batch-size}") int batchSize,
            @Value("${blog.image-storage.cleanup.max-batches-per-run}") int maxBatchesPerRun
    ) {
        this.outboxRepository = outboxRepository;
        this.imageStorage = imageStorage;
        this.batchSize = batchSize;
        this.maxBatchesPerRun = maxBatchesPerRun;
    }

    @Scheduled(
            fixedDelayString = "${blog.image-storage.cleanup.fixed-delay-ms}",
            initialDelayString = "${blog.image-storage.cleanup.initial-delay-ms}"
    )
    public void processPendingTasks() {
        for (int batch = 0; batch < maxBatchesPerRun; batch++) {
            List<ImageCleanupTask> pending = outboxRepository.findPending(batchSize);
            if (pending.isEmpty()) {
                return;
            }
            for (ImageCleanupTask task : pending) {
                process(task);
            }
        }
        log.info("Image cleanup batch limit reached; remaining tasks will be processed later");
    }

    private void process(ImageCleanupTask task) {
        try {
            imageStorage.delete(task.imagePath());
            outboxRepository.deleteById(task.id());
        } catch (RuntimeException exception) {
            String error = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            outboxRepository.recordFailure(task.id(), error);
            log.warn("Image cleanup task failed and will be retried: taskId={}, path={}",
                    task.id(), task.imagePath(), exception);
        }
    }
}
