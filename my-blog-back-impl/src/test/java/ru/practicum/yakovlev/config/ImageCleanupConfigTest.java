package ru.practicum.yakovlev.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.yakovlev.model.ImageCleanupTask;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;
import ru.practicum.yakovlev.storage.ImageStorage;

import java.util.List;
import java.util.stream.LongStream;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageCleanupConfigTest {

    @Mock
    private ImageCleanupOutboxRepository outboxRepository;
    @Mock
    private ImageStorage imageStorage;
    private ImageCleanupConfig imageCleanupConfig;

    @BeforeEach
    void setUp() {
        imageCleanupConfig = new ImageCleanupConfig(outboxRepository, imageStorage, 100, 10);
    }

    @Test
    @DisplayName("Processes no more than ten batches per run")
    void shouldLimitBatchesPerRun() {
        List<ImageCleanupTask> batch = LongStream.rangeClosed(1, 100)
                .mapToObj(id -> new ImageCleanupTask(id, id + ".png", 0, null))
                .toList();
        when(outboxRepository.findPending(100)).thenReturn(batch);

        imageCleanupConfig.processPendingTasks();

        verify(outboxRepository, times(10)).findPending(100);
        verify(imageStorage, times(1000)).delete(anyString());
        verify(outboxRepository, times(1000)).deleteById(anyLong());
    }
}
