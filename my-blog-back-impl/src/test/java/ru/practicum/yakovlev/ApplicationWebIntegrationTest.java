package ru.practicum.yakovlev;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(MyBlog.class)
class ApplicationWebIntegrationTest extends AbstractIntegrationTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.getJdbcTemplate().execute(
                "TRUNCATE TABLE post_tags, comments, tags, posts RESTART IDENTITY CASCADE"
        );
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Executes a complete HTTP scenario backed by PostgreSQL")
    void shouldExecuteCompleteHttpScenarioBackedByPostgres() throws Exception {
        JsonNode createdPost = responseBody(mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Spring context","text":"integration","tags":["#spring"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertAll(
                () -> assertEquals(1L, createdPost.path("id").longValue()),
                () -> assertEquals("Spring context", createdPost.path("title").stringValue()),
                () -> assertEquals("integration", createdPost.path("text").stringValue()),
                () -> assertEquals(1, createdPost.path("tags").size()),
                () -> assertEquals("#spring", createdPost.path("tags").get(0).stringValue()),
                () -> assertEquals(0L, createdPost.path("likesCount").longValue()),
                () -> assertEquals(0L, createdPost.path("commentsCount").longValue())
        );

        JsonNode createdComment = responseBody(mockMvc.perform(post("/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"wired comment"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertAll(
                () -> assertEquals(1L, createdComment.path("id").longValue()),
                () -> assertEquals("wired comment", createdComment.path("text").stringValue()),
                () -> assertEquals(1L, createdComment.path("postId").longValue())
        );

        JsonNode page = responseBody(mockMvc.perform(get("/posts")
                        .param("search", "#spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertEquals(1, page.path("posts").size());

        JsonNode foundPost = page.path("posts").get(0);
        assertAll(
                () -> assertEquals(1L, foundPost.path("id").longValue()),
                () -> assertEquals("Spring context", foundPost.path("title").stringValue()),
                () -> assertEquals("integration", foundPost.path("text").stringValue()),
                () -> assertEquals(1, foundPost.path("tags").size()),
                () -> assertEquals("#spring", foundPost.path("tags").get(0).stringValue()),
                () -> assertEquals(0L, foundPost.path("likesCount").longValue()),
                () -> assertEquals(1L, foundPost.path("commentsCount").longValue()),
                () -> assertFalse(page.path("hasPrev").booleanValue()),
                () -> assertFalse(page.path("hasNext").booleanValue()),
                () -> assertEquals(1L, page.path("lastPage").longValue())
        );
    }

    private JsonNode responseBody(ResultActions resultActions) throws Exception {
        return JSON_MAPPER.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
    }
}
