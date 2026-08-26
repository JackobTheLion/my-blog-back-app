package ru.practicum.yakovlev.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.yakovlev.AbstractIntegrationTest;
import ru.practicum.yakovlev.config.DatabaseConfig;
import ru.practicum.yakovlev.repository.impl.JdbcImageCleanupOutboxRepository;
import ru.practicum.yakovlev.repository.impl.JdbcPostRepository;
import ru.practicum.yakovlev.repository.impl.JdbcTagRepository;
import ru.practicum.yakovlev.service.PostImageUpdateService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {
        DatabaseConfig.class,
        JdbcTagRepository.class,
        JdbcPostRepository.class,
        JdbcImageCleanupOutboxRepository.class,
        PostImageUpdateServiceImpl.class
})
class PostImageUpdateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private PostImageUpdateService postImageUpdateService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE image_cleanup_outbox, post_tags, comments, tags, posts RESTART IDENTITY CASCADE"
        );
    }

    @Test
    @DisplayName("Updates the image path and enqueues old image cleanup in one transaction")
    void shouldUpdatePathAndEnqueueCleanup() {
        insertPost("1/old.png");

        postImageUpdateService.updateImagePath(1L, "1/new.png");

        assertEquals("1/new.png", currentImagePath());
        assertEquals(List.of("1/old.png"), cleanupPaths());
    }

    @Test
    @DisplayName("Rolls back the image path and cleanup task together")
    void shouldRollbackPathAndCleanupTogether() {
        insertPost("1/old.png");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            postImageUpdateService.updateImagePath(1L, "1/new.png");
            status.setRollbackOnly();
        });

        assertEquals("1/old.png", currentImagePath());
        assertTrue(cleanupPaths().isEmpty());
    }

    @Test
    @DisplayName("Does not enqueue cleanup when the post had no image")
    void shouldNotEnqueueCleanupWithoutOldImage() {
        insertPost(null);

        postImageUpdateService.updateImagePath(1L, "1/new.png");

        assertEquals("1/new.png", currentImagePath());
        assertTrue(cleanupPaths().isEmpty());
    }

    private void insertPost(String imagePath) {
        jdbcTemplate.update("""
                INSERT INTO posts (title, text, image_path)
                VALUES ('post', 'text', :imagePath)
                """, new MapSqlParameterSource()
                .addValue("imagePath", imagePath));
    }

    private String currentImagePath() {
        return jdbcTemplate.queryForObject(
                "SELECT image_path FROM posts WHERE id = 1",
                Map.of(),
                String.class
        );
    }

    private List<String> cleanupPaths() {
        return jdbcTemplate.queryForList(
                "SELECT image_path FROM image_cleanup_outbox ORDER BY id",
                Map.of(),
                String.class
        );
    }
}
