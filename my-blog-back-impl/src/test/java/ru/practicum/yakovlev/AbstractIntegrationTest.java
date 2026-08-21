package ru.practicum.yakovlev;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES = startPostgres();

    @DynamicPropertySource
    protected static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("blog.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("blog.datasource.username", POSTGRES::getUsername);
        registry.add("blog.datasource.password", POSTGRES::getPassword);
        registry.add("blog.datasource.schema", () -> "my_blog");
        registry.add(
                "blog.image-storage.directory",
                () -> Path.of("target", "integration-test-images").toAbsolutePath().toString()
        );
    }

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("blog_test")
                .withUsername("blog")
                .withPassword("blog");
        postgres.start();
        return postgres;
    }
}
