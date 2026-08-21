package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.practicum.yakovlev.config.DatabaseConfig;
import ru.practicum.yakovlev.AbstractIntegrationTest;

@SpringJUnitConfig(classes = {
        DatabaseConfig.class,
        JdbcTagRepository.class,
        JdbcPostRepository.class,
        JdbcCommentRepository.class
})
abstract class AbstractRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    protected JdbcTagRepository tagRepository;

    @Autowired
    protected JdbcPostRepository postRepository;

    @Autowired
    protected JdbcCommentRepository commentRepository;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE post_tags, comments, tags, posts RESTART IDENTITY CASCADE"
        );
    }
}
