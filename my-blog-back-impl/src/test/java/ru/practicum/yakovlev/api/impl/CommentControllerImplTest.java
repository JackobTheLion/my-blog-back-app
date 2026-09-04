package ru.practicum.yakovlev.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;
import ru.practicum.yakovlev.exception.CommentNotFoundException;
import ru.practicum.yakovlev.service.CommentService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.yakovlev.api.ApiConstants.POSTS_BASE_PATH;

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

        JsonNode comments = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/10/comments"))
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

        JsonNode comment = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/10/comments/20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).getComment(10L, 20L);
    }

    @Test
    @DisplayName("Deserializes a comment and uses the post path identifier")
    void shouldDeserializeCommentAndUsePostPathId() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("comment", 10L);
        when(commentService.createComment(request, 10L)).thenReturn(commentDto());

        JsonNode comment = responseBody(mockMvc.perform(post(POSTS_BASE_PATH + "/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"comment","postId":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).createComment(request, 10L);
    }

    @Test
    @DisplayName("Rejects comment creation without a post identifier")
    void shouldRejectCommentCreationWithoutPostId() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH + "/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"comment"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Returns HTTP 400 when comment creation post identifiers differ")
    void shouldReturnBadRequestWhenCreationPostIdentifiersDiffer() throws Exception {
        doThrow(new IllegalArgumentException("Post id in path and body must match"))
                .when(commentService).createComment(any(), eq(10L));

        mockMvc.perform(post(POSTS_BASE_PATH + "/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"comment","postId":11}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rejects a malformed comment body")
    void shouldRejectMalformedCommentBody() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH + "/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Rejects a blank comment")
    void shouldRejectBlankComment() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH + "/10/comments")
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
        mockMvc.perform(get(POSTS_BASE_PATH + "/0/comments"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Binds the request body and path variables when updating a comment")
    void shouldBindBodyAndPathVariablesWhenUpdatingComment() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest(20L, "updated", 10L);
        when(commentService.update(request, 10L, 20L)).thenReturn(commentDto());

        JsonNode comment = responseBody(mockMvc.perform(put(POSTS_BASE_PATH + "/10/comments/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":20,"text":"updated","postId":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertComment(comment);
        verify(commentService).update(request, 10L, 20L);
    }

    @Test
    @DisplayName("Rejects comment update without a body identifier")
    void shouldRejectCommentUpdateWithoutBodyId() throws Exception {
        mockMvc.perform(put(POSTS_BASE_PATH + "/10/comments/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"updated","postId":10}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("Returns HTTP 400 when comment update identifiers differ")
    void shouldReturnBadRequestWhenUpdateIdentifiersDiffer() throws Exception {
        doThrow(new IllegalArgumentException("Comment id in path and body must match"))
                .when(commentService).update(any(), eq(10L), eq(20L));

        mockMvc.perform(put(POSTS_BASE_PATH + "/10/comments/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":21,"text":"updated","postId":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delegates both path variables when deleting a comment")
    void shouldDelegatePathVariablesWhenDeletingComment() throws Exception {
        mockMvc.perform(delete(POSTS_BASE_PATH + "/10/comments/20"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));

        verify(commentService).deleteComment(10L, 20L);
    }

    @Test
    @DisplayName("Converts a missing comment exception to HTTP 404")
    void shouldConvertMissingCommentToNotFound() throws Exception {
        when(commentService.getComment(10L, 404L))
                .thenThrow(new CommentNotFoundException("Comment was not found"));

        JsonNode problem = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/10/comments/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

        assertAll(
                () -> assertEquals("Not Found", problem.path("title").textValue()),
                () -> assertEquals(404, problem.path("status").intValue()),
                () -> assertEquals("Comment was not found", problem.path("detail").textValue())
        );
    }

    private CommentResponseDto commentDto() {
        return new CommentResponseDto(20L, "comment", 10L);
    }

    private static JsonNode responseBody(ResultActions resultActions) throws IOException {
        return JSON_MAPPER.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
    }

    private static void assertComment(JsonNode comment) {
        assertAll(
                () -> assertEquals(20L, comment.path("id").longValue()),
                () -> assertEquals("comment", comment.path("text").textValue()),
                () -> assertEquals(10L, comment.path("postId").longValue())
        );
    }
}
