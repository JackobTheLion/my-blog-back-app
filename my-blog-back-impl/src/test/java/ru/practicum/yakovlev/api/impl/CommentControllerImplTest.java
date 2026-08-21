package ru.practicum.yakovlev.api.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.yakovlev.dto.CommentRequestDto;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.exception.CommentNotFoundException;
import ru.practicum.yakovlev.service.CommentService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerImplTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentControllerImpl(commentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Binds the post identifier and serializes comments")
    void shouldBindPostIdAndSerializeComments() throws Exception {
        when(commentService.findAllByPostId(10L)).thenReturn(List.of(commentDto()));

        JsonNode comments = responseBody(mockMvc.perform(get("/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertTrue(comments.isArray());
        assertEquals(1, comments.size());
        assertComment(comments.get(0));
        verify(commentService).findAllByPostId(10L);
    }

    @Test
    @DisplayName("Binds post and comment path variables")
    void shouldBindPostAndCommentPathVariables() throws Exception {
        when(commentService.getComment(10L, 20L)).thenReturn(commentDto());

        JsonNode comment = responseBody(mockMvc.perform(get("/posts/10/comments/20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).getComment(10L, 20L);
    }

    @Test
    @DisplayName("Deserializes a comment and uses the post path identifier")
    void shouldDeserializeCommentAndUsePostPathId() throws Exception {
        CommentRequestDto request = new CommentRequestDto("comment", null);
        when(commentService.createComment(request, 10L)).thenReturn(commentDto());

        JsonNode comment = responseBody(mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"comment","postId":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).createComment(request, 10L);
    }

    @Test
    @DisplayName("Rejects a malformed comment body")
    void shouldRejectMalformedCommentBody() throws Exception {
        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Rejects a blank comment")
    void shouldRejectBlankComment() throws Exception {
        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":" ","postId":10}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Rejects a non-positive path identifier")
    void shouldRejectNonPositivePathId() throws Exception {
        mockMvc.perform(get("/posts/0/comments"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Binds the request body and path variables when updating a comment")
    void shouldBindBodyAndPathVariablesWhenUpdatingComment() throws Exception {
        CommentRequestDto request = new CommentRequestDto("updated", 10L);
        when(commentService.update(request, 10L, 20L)).thenReturn(commentDto());

        JsonNode comment = responseBody(mockMvc.perform(put("/posts/10/comments/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"updated","postId":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).update(request, 10L, 20L);
    }

    @Test
    @DisplayName("Delegates both path variables when deleting a comment")
    void shouldDelegatePathVariablesWhenDeletingComment() throws Exception {
        mockMvc.perform(delete("/posts/10/comments/20"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));

        verify(commentService).deleteComment(10L, 20L);
    }

    @Test
    @DisplayName("Converts a missing comment exception to HTTP 404")
    void shouldConvertMissingCommentToNotFound() throws Exception {
        when(commentService.getComment(10L, 404L))
                .thenThrow(new CommentNotFoundException("Comment was not found"));

        JsonNode problem = responseBody(mockMvc.perform(get("/posts/10/comments/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

        assertAll(
                () -> assertEquals("Not Found", problem.path("title").stringValue()),
                () -> assertEquals(404, problem.path("status").intValue()),
                () -> assertEquals("Comment was not found", problem.path("detail").stringValue())
        );
    }

    private CommentResponseDto commentDto() {
        return new CommentResponseDto(20L, "comment", 10L);
    }

    private static JsonNode responseBody(ResultActions resultActions) throws Exception {
        return JSON_MAPPER.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
    }

    private static void assertComment(JsonNode comment) {
        assertAll(
                () -> assertEquals(20L, comment.path("id").longValue()),
                () -> assertEquals("comment", comment.path("text").stringValue()),
                () -> assertEquals(10L, comment.path("postId").longValue())
        );
    }
}
