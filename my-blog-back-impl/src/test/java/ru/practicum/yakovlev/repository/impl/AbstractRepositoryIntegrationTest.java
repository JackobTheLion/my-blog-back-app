package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.practicum.yakovlev.AbstractIntegrationTest;

abstract class AbstractRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    protected JdbcTagRepository tagRepository;

    @Autowired
    protected JdbcPostRepository postRepository;

    @Autowired
    protected JdbcCommentRepository commentRepository;

    @Autowired
    protected JdbcImageCleanupOutboxRepository imageCleanupOutboxRepository;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE image_cleanup_outbox, post_tags, comments, tags, posts RESTART IDENTITY CASCADE"
        );
    }
}
