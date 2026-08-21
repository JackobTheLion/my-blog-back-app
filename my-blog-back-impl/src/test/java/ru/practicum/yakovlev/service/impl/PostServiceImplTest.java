package ru.practicum.yakovlev.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.PostRequestDto;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.mapper.PostMapperImpl;
import ru.practicum.yakovlev.mapper.TagMapperImpl;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.storage.ImageStorage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {PostServiceImpl.class, PostMapperImpl.class, TagMapperImpl.class})
class PostServiceImplTest {

    @MockitoBean
    private PostRepository postRepository;
    @MockitoBean
    private ImageStorage imageStorage;

    @Autowired
    private PostServiceImpl postService;

    private final MultipartFile image = mock(MultipartFile.class);

    @Test
    @DisplayName("Returns mapped posts with pagination flags")
    void shouldReturnMappedPostsWithPaginationFlags() {
        Post first = newPost(1L, "first");
        Post second = newPost(2L, "second");
        FullPostResponseDto firstDto = newPostDto(1L, "first");
        FullPostResponseDto secondDto = newPostDto(2L, "second");
        when(postRepository.findAll(any(), eq(2), eq(2))).thenReturn(List.of(first, second));
        when(postRepository.count(any())).thenReturn(5L);

        PageResponse result = postService.getPosts("Spring #java", 2, 2);

        assertAll(
                () -> assertEquals(List.of(firstDto, secondDto), result.posts()),
                () -> assertTrue(result.hasPrev()),
                () -> assertTrue(result.hasNext()),
                () -> assertEquals(3L, result.lastPage())
        );
    }

    @Test
    @DisplayName("Returns an empty page when no posts are found")
    void shouldReturnEmptyPageWhenNoPostsAreFound() {
        when(postRepository.findAll(any(), eq(1), eq(10))).thenReturn(List.of());
        when(postRepository.count(any())).thenReturn(0L);

        PageResponse result = postService.getPosts(null, 1, 10);

        assertTrue(result.posts().isEmpty());
        assertFalse(result.hasPrev());
        assertFalse(result.hasNext());
        assertEquals(0L, result.lastPage());
    }

    @Test
    @DisplayName("Creates and returns a post")
    void shouldCreateAndReturnPost() {
        PostRequestDto request = new PostRequestDto(null, "title", "text", List.of("#java"));
        Post saved = newPost(1L, "title");
        FullPostResponseDto response = newPostDto(1L, "title");
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        assertEquals(response, postService.createPost(request));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("Updates an existing post when identifiers match")
    void shouldUpdateExistingPostWhenIdentifiersMatch() {
        PostRequestDto request = new PostRequestDto(1L, "new", "new text", List.of());
        Post existing = newPost(1L, "old");
        FullPostResponseDto response = new FullPostResponseDto(1L, "new", "new text", List.of(), 0L, 0L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(response, postService.updatePost(1L, request));
        verify(postRepository, times(1)).update(any(Post.class));
    }

    @Test
    @DisplayName("Rejects an update when path and body identifiers differ")
    void shouldRejectUpdateWhenPathAndBodyIdentifiersDiffer() {
        PostRequestDto request = new PostRequestDto(2L, "title", "text", List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> postService.updatePost(1L, request)
        );

        assertEquals("Post id not match", exception.getMessage());
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("Returns a post by identifier")
    void shouldReturnPostById() {
        Post entity = newPost(1L, "post");
        FullPostResponseDto response = newPostDto(1L, "post");
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertEquals(response, postService.findPost(1L));
    }

    @Test
    @DisplayName("Throws when the requested post does not exist")
    void shouldThrowWhenPostDoesNotExist() {
        when(postRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.findPost(404L));
    }

    @Test
    @DisplayName("Deletes a post and its stored image")
    void shouldDeletePostAndStoredImage() {
        Post entity = newPost(1L, "post");
        entity.setImagePath("1/image.png");
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));

        postService.deletePost(1L);

        verify(postRepository).deleteById(1L);
        verify(imageStorage).delete("1/image.png");
    }

    @Test
    @DisplayName("Throws when deleting a missing post")
    void shouldThrowWhenDeletingMissingPost() {
        when(postRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.deletePost(404L));

        verify(postRepository, never()).deleteById(anyLong());
        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("Increments likes for an existing post")
    void shouldIncrementLikesForExistingPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(newPost(1L, "post")));
        when(postRepository.incrementLikes(1L)).thenReturn(7L);

        assertEquals(7L, postService.likePost(1L));
    }

    @Test
    @DisplayName("Does not increment likes for a missing post")
    void shouldNotIncrementLikesForMissingPost() {
        when(postRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.likePost(404L));

        verify(postRepository, never()).incrementLikes(anyLong());
    }

    @Test
    @DisplayName("Replaces a post image and deletes the old file")
    void shouldReplacePostImageAndDeleteOldFile() throws IOException {
        Post entity = newPost(1L, "post");
        entity.setImagePath("1/old.png");
        byte[] content = {1, 2, 3};
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(image.getOriginalFilename()).thenReturn("new.png");
        when(image.getBytes()).thenReturn(content);
        when(imageStorage.save(1L, "new.png", content)).thenReturn("1/new.png");

        postService.savePostImage(1L, image);

        verify(postRepository).updateImagePath(1L, "1/new.png");
        verify(imageStorage).delete("1/old.png");
    }

    @Test
    @DisplayName("Deletes the new image when the database update fails")
    void shouldDeleteNewImageWhenDatabaseUpdateFails() throws IOException {
        byte[] content = {1, 2, 3};
        Post entity = newPost(1L, "post");
        entity.setImagePath("1/old.png");
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(image.getOriginalFilename()).thenReturn("new.png");
        when(image.getBytes()).thenReturn(content);
        when(imageStorage.save(1L, "new.png", content)).thenReturn("1/new.png");
        doThrow(new IllegalStateException("database unavailable"))
                .when(postRepository).updateImagePath(1L, "1/new.png");

        assertThrows(IllegalStateException.class, () -> postService.savePostImage(1L, image));

        verify(imageStorage).delete("1/new.png");
        verify(imageStorage, never()).delete("1/old.png");
    }

    @Test
    @DisplayName("Returns a stored post image")
    void shouldReturnStoredPostImage() {
        byte[] content = {4, 5};
        when(postRepository.findImagePathByPostId(1L)).thenReturn(Optional.of("1/image.png"));
        when(imageStorage.read("1/image.png")).thenReturn(content);

        assertArrayEquals(content, postService.findPostImage(1L));
    }

    @Test
    @DisplayName("Throws when a post image does not exist")
    void shouldThrowWhenPostImageDoesNotExist() {
        when(postRepository.findImagePathByPostId(1L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.findPostImage(1L));
        verifyNoInteractions(imageStorage);
    }

    private Post newPost(Long id, String title) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setText("text");
        return post;
    }

    private FullPostResponseDto newPostDto(Long id, String title) {
        return new FullPostResponseDto(id, title, "text", List.of(), 0L, 0L);
    }

}
