package ru.practicum.yakovlev.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;
import ru.practicum.yakovlev.exception.CommentNotFoundException;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.mapper.CommentMapperImpl;
import ru.practicum.yakovlev.model.Comment;
import ru.practicum.yakovlev.repository.CommentRepository;
import ru.practicum.yakovlev.repository.PostRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {CommentServiceImpl.class, CommentMapperImpl.class})
class CommentServiceImplTest {

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private PostRepository postRepository;

    @Autowired
    private CommentServiceImpl commentService;

    @Test
    @DisplayName("Returns all comments for a post")
    void shouldReturnAllCommentsForPost() {
        List<Comment> entities = List.of(newComment(1L, "first"), newComment(2L, "second"));
        List<CommentResponseDto> response = List.of(newCommentDto(1L, "first"), newCommentDto(2L, "second"));
        when(commentRepository.findAllByPostId(10L)).thenReturn(entities);

        assertEquals(response, commentService.findAllByPostId(10L));
    }

    @Test
    @DisplayName("Returns a comment by post and comment identifiers")
    void shouldReturnCommentByPostAndCommentIds() {
        Comment entity = newComment(20L, "comment");
        CommentResponseDto response = newCommentDto(20L, "comment");
        when(commentRepository.findByIdAndPostId(10L, 20L)).thenReturn(Optional.of(entity));

        assertEquals(response, commentService.getComment(10L, 20L));
        verify(commentRepository).findByIdAndPostId(10L, 20L);
    }

    @Test
    @DisplayName("Throws when a comment does not belong to the post")
    void shouldThrowWhenCommentDoesNotBelongToPost() {
        when(commentRepository.findByIdAndPostId(10L, 404L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.getComment(10L, 404L));
    }

    @Test
    @DisplayName("Creates and returns a comment")
    void shouldCreateAndReturnComment() {
        CreateCommentRequest request = new CreateCommentRequest("comment", 10L);
        Comment saved = newComment(20L, "comment");
        CommentResponseDto response = newCommentDto(20L, "comment");
        when(postRepository.isPostExists(10L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        assertEquals(response, commentService.createComment(request, 10L));
        verify(postRepository).isPostExists(10L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Throws when creating a comment for a missing post")
    void shouldThrowWhenCreatingCommentForMissingPost() {
        CreateCommentRequest request = new CreateCommentRequest("comment", 404L);
        when(postRepository.isPostExists(404L)).thenReturn(false);

        PostNotFoundException exception = assertThrows(
                PostNotFoundException.class,
                () -> commentService.createComment(request, 404L)
        );

        assertEquals("Post with id 404 was not found", exception.getMessage());
        verify(postRepository).isPostExists(404L);
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("Converts a comments post foreign key violation to a missing post error")
    void shouldConvertCommentsPostForeignKeyViolationToMissingPost() {
        CreateCommentRequest request = new CreateCommentRequest("comment", 10L);
        SQLException sqlException = new SQLException(
                "Foreign key violation",
                PSQLState.FOREIGN_KEY_VIOLATION.getState()
        );
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Constraint violation",
                sqlException
        );
        when(postRepository.isPostExists(10L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenThrow(violation);

        PostNotFoundException exception = assertThrows(
                PostNotFoundException.class,
                () -> commentService.createComment(request, 10L)
        );

        assertEquals("Post with id 10 was not found", exception.getMessage());
    }

    @Test
    @DisplayName("Updates an existing comment")
    void shouldUpdateExistingComment() {
        UpdateCommentRequest request = new UpdateCommentRequest(20L, "new", 10L);
        Comment existing = newComment(20L, "old");
        CommentResponseDto response = newCommentDto(20L, "new");
        when(commentRepository.findByIdAndPostId(10L, 20L)).thenReturn(Optional.of(existing));
        when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(response, commentService.update(request, 10L, 20L));
        verify(commentRepository).update(any(Comment.class));
    }

    @Test
    @DisplayName("Rejects comment creation when post identifiers differ")
    void shouldRejectCreationWhenPostIdentifiersDiffer() {
        CreateCommentRequest request = new CreateCommentRequest("comment", 11L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(request, 10L)
        );

        assertEquals("Post id in path and body must match", exception.getMessage());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("Rejects comment update when post identifiers differ")
    void shouldRejectUpdateWhenPostIdentifiersDiffer() {
        UpdateCommentRequest request = new UpdateCommentRequest(20L, "new", 11L);

        assertThrows(
                IllegalArgumentException.class,
                () -> commentService.update(request, 10L, 20L)
        );

        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("Rejects comment update when comment identifiers differ")
    void shouldRejectUpdateWhenCommentIdentifiersDiffer() {
        UpdateCommentRequest request = new UpdateCommentRequest(21L, "new", 10L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.update(request, 10L, 20L)
        );

        assertEquals("Comment id in path and body must match", exception.getMessage());
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("Deletes a comment by post and comment identifiers")
    void shouldDeleteCommentByPostAndCommentIds() {
        commentService.deleteComment(10L, 20L);

        verify(commentRepository).deleteByIdAndPostId(10L, 20L);
    }

    private Comment newComment(Long id, String text) {
        return new Comment(id, text, 10L);
    }

    private CommentResponseDto newCommentDto(Long id, String text) {
        return new CommentResponseDto(id, text, 10L);
    }
}
