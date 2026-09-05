package ru.practicum.yakovlev.service.impl;

import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.config.AllowedImageTypes;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.mapper.PostMapperImpl;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.service.PostImageUpdateService;
import ru.practicum.yakovlev.storage.ImageStorage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private ImageStorage imageStorage;
    @Mock
    private ImageCleanupOutboxRepository outboxRepository;
    @Mock
    private PostImageUpdateService postImageUpdateService;
    @Mock
    private Tika tika;
    @Mock
    private MultipartFile image;

    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        PostMapperImpl postMapper = new PostMapperImpl();
        postService = new PostServiceImpl(
                postRepository,
                postMapper,
                imageStorage,
                outboxRepository,
                postImageUpdateService,
                tika,
                new AllowedImageTypes(Set.of("image/png", "image/jpeg", "image/gif"))
        );
    }

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
        CreatePostRequest request = new CreatePostRequest("title", "text", List.of("#java"));
        Post saved = newPost(1L, "title");
        FullPostResponseDto response = newPostDto(1L, "title");
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        assertEquals(response, postService.createPost(request));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("Updates an existing post when identifiers match")
    void shouldUpdateExistingPostWhenIdentifiersMatch() {
        UpdatePostRequest request = new UpdatePostRequest(1L, "new", "new text", List.of());
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
        UpdatePostRequest request = new UpdatePostRequest(2L, "title", "text", List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.updatePost(1L, request)
        );

        assertEquals("Post id in path and body must match", exception.getMessage());
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
        verify(outboxRepository).enqueueDelete("1/image.png");
    }

    @Test
    @DisplayName("Throws when deleting a missing post")
    void shouldThrowWhenDeletingMissingPost() {
        when(postRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.deletePost(404L));

        verify(postRepository, never()).deleteById(anyLong());
        verifyNoInteractions(outboxRepository);
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
    @DisplayName("Saves a new image before updating its database path")
    void shouldSaveNewImageBeforeUpdatingDatabasePath() throws IOException {
        Post entity = newPost(1L, "post");
        entity.setImagePath("1/old.png");
        byte[] content = {1, 2, 3};
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(image.getOriginalFilename()).thenReturn("new.png");
        when(image.getBytes()).thenReturn(content);
        when(tika.detect(content)).thenReturn("image/png");
        when(imageStorage.save(1L, "new.png", content)).thenReturn("1/new.png");

        postService.savePostImage(1L, image);

        InOrder inOrder = inOrder(imageStorage, postImageUpdateService);
        inOrder.verify(imageStorage).save(1L, "new.png", content);
        inOrder.verify(postImageUpdateService).updateImagePath(1L, "1/new.png");
    }

    @Test
    @DisplayName("Deletes the new image when its database transaction fails")
    void shouldDeleteNewImageWhenDatabaseTransactionFails() throws IOException {
        byte[] content = {1, 2, 3};
        Post entity = newPost(1L, "post");
        entity.setImagePath("1/old.png");
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(image.getOriginalFilename()).thenReturn("new.png");
        when(image.getBytes()).thenReturn(content);
        when(tika.detect(content)).thenReturn("image/png");
        when(imageStorage.save(1L, "new.png", content)).thenReturn("1/new.png");
        doThrow(new IllegalStateException("database unavailable"))
                .when(postImageUpdateService).updateImagePath(1L, "1/new.png");

        assertThrows(
                IllegalStateException.class,
                () -> postService.savePostImage(1L, image)
        );
        verify(imageStorage).delete("1/new.png");
    }

    @Test
    @DisplayName("Does not update the database when saving the new image fails")
    void shouldNotUpdateDatabaseWhenSavingImageFails() throws IOException {
        byte[] content = {1, 2, 3};
        Post entity = newPost(1L, "post");
        when(postRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(image.getOriginalFilename()).thenReturn("new.png");
        when(image.getBytes()).thenReturn(content);
        when(tika.detect(content)).thenReturn("image/png");
        doThrow(new IllegalStateException("disk unavailable"))
                .when(imageStorage).save(1L, "new.png", content);

        assertThrows(
                IllegalStateException.class,
                () -> postService.savePostImage(1L, image)
        );
        verifyNoInteractions(postImageUpdateService);
    }

    @Test
    @DisplayName("Rejects uploaded content that is not an allowed image")
    void shouldRejectUnsupportedImageContent() throws IOException {
        byte[] content = {1, 2, 3};
        when(postRepository.findById(1L)).thenReturn(Optional.of(newPost(1L, "post")));
        when(image.getBytes()).thenReturn(content);
        when(tika.detect(content)).thenReturn("application/octet-stream");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.savePostImage(1L, image)
        );

        assertEquals("Image type is not allowed: application/octet-stream", exception.getMessage());
        verifyNoInteractions(imageStorage, postImageUpdateService);
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
