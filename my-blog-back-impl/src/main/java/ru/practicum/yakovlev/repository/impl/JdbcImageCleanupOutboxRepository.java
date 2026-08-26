package ru.practicum.yakovlev.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.practicum.yakovlev.model.ImageCleanupTask;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcImageCleanupOutboxRepository implements ImageCleanupOutboxRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void enqueueDelete(String imagePath) {
        jdbcTemplate.update("""
                INSERT INTO image_cleanup_outbox (image_path)
                VALUES (:imagePath)
                ON CONFLICT (image_path) DO NOTHING
                """, Map.of("imagePath", imagePath));
    }

    @Override
    public List<ImageCleanupTask> findPending(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, image_path, attempts, last_error
                        FROM image_cleanup_outbox
                        WHERE next_attempt_at <= CURRENT_TIMESTAMP
                        ORDER BY id
                        LIMIT :limit
                        """,
                Map.of("limit", limit),
                (resultSet, rowNumber) -> new ImageCleanupTask(
                        resultSet.getLong("id"),
                        resultSet.getString("image_path"),
                        resultSet.getInt("attempts"),
                        resultSet.getString("last_error")
                ));
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM image_cleanup_outbox WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void recordFailure(Long id, String error) {
        jdbcTemplate.update("""
                UPDATE image_cleanup_outbox
                SET attempts = attempts + 1,
                    last_error = :error,
                    next_attempt_at = CURRENT_TIMESTAMP + INTERVAL '1 minute'
                WHERE id = :id
                """, Map.of("id", id, "error", error));
    }
}
