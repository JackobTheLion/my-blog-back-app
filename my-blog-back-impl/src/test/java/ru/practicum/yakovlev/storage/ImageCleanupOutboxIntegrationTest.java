package ru.practicum.yakovlev.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.practicum.yakovlev.AbstractIntegrationTest;
import ru.practicum.yakovlev.config.DatabaseConfig;
import ru.practicum.yakovlev.config.ImageCleanupConfig;
import ru.practicum.yakovlev.repository.impl.JdbcImageCleanupOutboxRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {
        DatabaseConfig.class,
        JdbcImageCleanupOutboxRepository.class,
        ImageCleanupConfig.class
})
@TestPropertySource("classpath:application.properties")
class ImageCleanupOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private JdbcImageCleanupOutboxRepository outboxRepository;
    @Autowired
    private ImageCleanupConfig imageCleanupConfig;
    @MockitoBean
    private ImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE image_cleanup_outbox RESTART IDENTITY"
        );
    }

    @Test
    @DisplayName("Deletes an image and removes its completed outbox task")
    void shouldProcessCleanupTask() {
        outboxRepository.enqueueDelete("1/image.png");

        imageCleanupConfig.processPendingTasks();

        verify(imageStorage).delete("1/image.png");
        assertTrue(outboxRepository.findPending(10).isEmpty());
    }

}
