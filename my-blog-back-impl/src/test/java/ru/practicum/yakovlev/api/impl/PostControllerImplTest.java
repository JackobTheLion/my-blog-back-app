package ru.practicum.yakovlev.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.service.PostService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.yakovlev.api.ApiConstants.POSTS_BASE_PATH;

@ExtendWith(MockitoExtension.class)
class PostControllerImplTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Mock
    private PostService postService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostControllerImpl(postService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Binds query parameters and serializes the posts page")
    void shouldBindQueryParametersAndSerializePostsPage() throws Exception {
        when(postService.getPosts("spring", 2, 10))
                .thenReturn(new PageResponse(List.of(postDto()), true, false, 2L));

        JsonNode page = responseBody(mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "spring")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        assertAll(
                () -> assertTrue(page.path("hasPrev").booleanValue()),
                () -> assertFalse(page.path("hasNext").booleanValue()),
                () -> assertEquals(2L, page.path("lastPage").longValue()),
                () -> assertEquals(1, page.path("posts").size())
        );
        assertPost(page.path("posts").get(0));
        verify(postService).getPosts("spring", 2, 10);
    }

    @Test
    @DisplayName("Rejects a request with a missing pagination parameter")
    void shouldRejectMissingPaginationParameter() throws Exception {
        mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "spring")
                        .param("pageNumber", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Rejects invalid pagination values")
    void shouldRejectInvalidPaginationValues() throws Exception {
        mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Accepts the maximum page size")
    void shouldAcceptMaximumPageSize() throws Exception {
        when(postService.getPosts("", 1, 100))
                .thenReturn(new PageResponse(List.of(), false, false, 0L));

        mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk());

        verify(postService).getPosts("", 1, 100);
    }

    @Test
    @DisplayName("Rejects a page size above the maximum")
    void shouldRejectPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Deserializes a post creation request")
    void shouldDeserializePostCreationRequest() throws Exception {
        when(postService.createPost(any())).thenReturn(postDto());

        JsonNode post = responseBody(mockMvc.perform(post(POSTS_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"title","text":"text","tags":["#java"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        verify(postService).createPost(new CreatePostRequest("title", "text", List.of("#java")));
        assertPost(post);
    }

    @Test
    @DisplayName("Rejects a malformed post body")
    @SuppressWarnings("JsonStandardCompliance")
    void shouldRejectMalformedPostBody() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Rejects invalid post fields")
    void shouldRejectInvalidPostFields() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","text":"","tags":[""]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Rejects post creation without tags")
    void shouldRejectPostCreationWithoutTags() throws Exception {
        mockMvc.perform(post(POSTS_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"title","text":"text"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Binds the path identifier and body when updating a post")
    void shouldBindPathIdAndBodyWhenUpdatingPost() throws Exception {
        when(postService.updatePost(eq(1L), any())).thenReturn(postDto());

        JsonNode post = responseBody(mockMvc.perform(put(POSTS_BASE_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"title":"title","text":"text","tags":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));

        verify(postService).updatePost(1L, new UpdatePostRequest(1L, "title", "text", List.of()));
        assertPost(post);
    }

    @Test
    @DisplayName("Rejects post update without a body identifier")
    void shouldRejectPostUpdateWithoutBodyId() throws Exception {
        mockMvc.perform(put(POSTS_BASE_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"title","text":"text","tags":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("Returns HTTP 400 when post identifiers differ")
    void shouldReturnBadRequestWhenPostIdentifiersDiffer() throws Exception {
        doThrow(new IllegalArgumentException("Post id in path and body must match"))
                .when(postService).updatePost(eq(1L), any());

        mockMvc.perform(put(POSTS_BASE_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":2,"title":"title","text":"text","tags":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delegates the post identifier when reading and deleting a post")
    void shouldDelegatePostIdWhenReadingAndDeletingPost() throws Exception {
        when(postService.findPost(1L)).thenReturn(postDto());

        JsonNode post = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
        assertPost(post);

        mockMvc.perform(delete(POSTS_BASE_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));

        verify(postService).findPost(1L);
        verify(postService).deletePost(1L);
    }

    @Test
    @DisplayName("Returns the updated likes counter")
    void shouldReturnUpdatedLikesCounter() throws Exception {
        when(postService.likePost(1L)).thenReturn(7L);

        JsonNode likesCount = responseBody(mockMvc.perform(post(POSTS_BASE_PATH + "/1/likes"))
                .andExpect(status().isOk()));

        assertEquals(7L, likesCount.longValue());
    }

    @Test
    @DisplayName("Binds a multipart file when updating a post image")
    void shouldBindMultipartFileWhenUpdatingPostImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2}
        );

        mockMvc.perform(multipart(HttpMethod.PUT, POSTS_BASE_PATH + "/1/image").file(image))
                .andExpect(status().isOk());

        verify(postService).savePostImage(1L, image);
    }

    @Test
    @DisplayName("Writes post image bytes to the response")
    void shouldWritePostImageBytesToResponse() throws Exception {
        byte[] image = {1, 2, 3};
        when(postService.findPostImage(1L)).thenReturn(image);

        mockMvc.perform(get(POSTS_BASE_PATH + "/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(image));
    }

    @Test
    @DisplayName("Converts a missing post exception to a problem detail")
    void shouldConvertMissingPostToProblemDetail() throws Exception {
        when(postService.findPost(404L)).thenThrow(new PostNotFoundException("Post 404 was not found"));

        JsonNode problem = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

        assertProblemDetail(problem, 404, "Not Found", "Post 404 was not found");
    }

    @Test
    @DisplayName("Converts an image storage exception to HTTP 500")
    void shouldConvertImageStorageExceptionToInternalServerError() throws Exception {
        when(postService.findPostImage(1L)).thenThrow(
                new ImageStorageException("Image cannot be read", new java.io.IOException("disk error"))
        );

        JsonNode problem = responseBody(mockMvc.perform(get(POSTS_BASE_PATH + "/1/image"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

        assertProblemDetail(problem, 500, "Internal Server Error", "Image cannot be read");
    }

    @Test
    @DisplayName("Converts an illegal argument exception to HTTP 400")
    void shouldConvertIllegalArgumentExceptionToBadRequest() throws Exception {
        when(postService.getPosts("invalid", 1, 10))
                .thenThrow(new IllegalArgumentException("Invalid search request"));

        JsonNode problem = responseBody(mockMvc.perform(get(POSTS_BASE_PATH)
                        .param("search", "invalid")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

        assertProblemDetail(problem, 400, "Bad Request", "Invalid search request");
    }

    private static FullPostResponseDto postDto() {
        return new FullPostResponseDto(1L, "title", "text", List.of("#java"), 2L, 3L);
    }

    private static JsonNode responseBody(ResultActions resultActions) throws IOException {
        return JSON_MAPPER.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
    }

    private static void assertPost(JsonNode post) {
        assertAll(
                () -> assertEquals(1L, post.path("id").longValue()),
                () -> assertEquals("title", post.path("title").textValue()),
                () -> assertEquals("text", post.path("text").textValue()),
                () -> assertEquals(1, post.path("tags").size()),
                () -> assertEquals("#java", post.path("tags").get(0).textValue()),
                () -> assertEquals(2L, post.path("likesCount").longValue()),
                () -> assertEquals(3L, post.path("commentsCount").longValue())
        );
    }

    private static void assertProblemDetail(JsonNode problem, int status, String title, String detail) {
        assertAll(
                () -> assertEquals(title, problem.path("title").textValue()),
                () -> assertEquals(status, problem.path("status").intValue()),
                () -> assertEquals(detail, problem.path("detail").textValue())
        );
    }
}
