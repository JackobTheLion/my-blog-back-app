package ru.practicum.yakovlev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.yakovlev.api.ApiConstants.POSTS_BASE_PATH;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationWebIntegrationTest extends AbstractIntegrationTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE image_cleanup_outbox, post_tags, comments, tags, posts RESTART IDENTITY CASCADE"
        );
    }

    @Test
    @DisplayName("Executes a complete HTTP scenario backed by PostgreSQL")
    void shouldExecuteCompleteHttpScenarioBackedByPostgres() throws Exception {
        JsonNode createdPost = responseBody(mockMvc.perform(post(POSTS_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Spring context","text":"integration","tags":["#spring"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertAll(
                () -> assertEquals(1L, createdPost.path("id").longValue()),
                () -> assertEquals("Spring context", createdPost.path("title").textValue()),
                () -> assertEquals("integration", createdPost.path("text").textValue()),
                () -> assertEquals(1, createdPost.path("tags").size()),
                () -> assertEquals("#spring", createdPost.path("tags").get(0).textValue()),
                () -> assertEquals(0L, createdPost.path("likesCount").longValue()),
                () -> assertEquals(0L, createdPost.path("commentsCount").longValue())
        );

        JsonNode createdComment = responseBody(mockMvc.perform(post(POSTS_BASE_PATH + "/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"wired comment","postId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertAll(
                () -> assertEquals(1L, createdComment.path("id").longValue()),
                () -> assertEquals("wired comment", createdComment.path("text").textValue()),
                () -> assertEquals(1L, createdComment.path("postId").longValue())
        );

        JsonNode page = responseBody(mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "#spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertEquals(1, page.path("posts").size());

        JsonNode foundPost = page.path("posts").get(0);
        assertAll(
                () -> assertEquals(1L, foundPost.path("id").longValue()),
                () -> assertEquals("Spring context", foundPost.path("title").textValue()),
                () -> assertEquals("integration", foundPost.path("text").textValue()),
                () -> assertEquals(1, foundPost.path("tags").size()),
                () -> assertEquals("#spring", foundPost.path("tags").get(0).textValue()),
                () -> assertEquals(0L, foundPost.path("likesCount").longValue()),
                () -> assertEquals(1L, foundPost.path("commentsCount").longValue()),
                () -> assertFalse(page.path("hasPrev").booleanValue()),
                () -> assertFalse(page.path("hasNext").booleanValue()),
                () -> assertEquals(1L, page.path("lastPage").longValue())
        );
    }

    @Test
    @DisplayName("Returns HTTP 404 when creating a comment for a missing post")
    void shouldReturnNotFoundWhenCreatingCommentForMissingPost() throws Exception {
        JsonNode problem = responseBody(mockMvc.perform(post(POSTS_BASE_PATH + "/404/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"orphan comment","postId":404}
                                """))
                .andExpect(status().isNotFound()));

        Long commentsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comments", Map.of(), Long.class);
        assertAll(
                () -> assertEquals(404, problem.path("status").intValue()),
                () -> assertEquals("Not Found", problem.path("title").textValue()),
                () -> assertEquals("Post with id 404 was not found", problem.path("detail").textValue()),
                () -> assertEquals(0L, commentsCount)
        );
    }

    private JsonNode responseBody(ResultActions resultActions) throws IOException {
        return JSON_MAPPER.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
    }
}
