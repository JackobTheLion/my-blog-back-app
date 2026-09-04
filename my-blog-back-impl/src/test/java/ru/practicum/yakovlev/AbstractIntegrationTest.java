package ru.practicum.yakovlev;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = startPostgres();

    @DynamicPropertySource
    protected static void integrationTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.schema", () -> "my_blog");
        registry.add(
                "blog.image-storage.directory",
                () -> Path.of("build", "integration-test-images").toAbsolutePath().toString()
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
