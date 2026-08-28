package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.model.Comment;
import ru.practicum.yakovlev.model.Post;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCommentRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Test
    @DisplayName("Saves comments and returns them in insertion order")
    void shouldSaveAndReturnCommentsInInsertionOrder() {
        long postId = savedPostId();
        Comment first = commentRepository.save(new Comment(null, "first", postId));
        Comment second = commentRepository.save(new Comment(null, "second", postId));

        List<Comment> comments = commentRepository.findAllByPostId(postId);

        assertEquals(List.of(first.getId(), second.getId()), comments.stream().map(Comment::getId).toList());
        assertEquals(List.of("first", "second"), comments.stream().map(Comment::getText).toList());
    }

    @Test
    @DisplayName("Finds a comment only when both identifiers match")
    void shouldFindCommentOnlyWhenBothIdsMatch() {
        long firstPostId = savedPostId();
        long secondPostId = savedPostId();
        Comment saved = commentRepository.save(new Comment(null, "comment", firstPostId));

        assertEquals(saved.getId(), commentRepository.findByIdAndPostId(firstPostId, saved.getId()).orElseThrow().getId());
        assertTrue(commentRepository.findByIdAndPostId(secondPostId, saved.getId()).isEmpty());
        assertTrue(commentRepository.findByIdAndPostId(firstPostId, 404L).isEmpty());
    }

    @Test
    @DisplayName("Updates a comment belonging to the specified post")
    void shouldUpdateCommentBelongingToSpecifiedPost() {
        long postId = savedPostId();
        Comment saved = commentRepository.save(new Comment(null, "old", postId));
        saved.setText("new");

        commentRepository.update(saved);

        assertEquals("new", commentRepository.findByIdAndPostId(postId, saved.getId()).orElseThrow().getText());
    }

    @Test
    @DisplayName("Does not update a comment for a different post")
    void shouldNotUpdateCommentForDifferentPost() {
        long ownerPostId = savedPostId();
        long otherPostId = savedPostId();
        Comment saved = commentRepository.save(new Comment(null, "old", ownerPostId));

        commentRepository.update(new Comment(saved.getId(), "new", otherPostId));

        assertEquals("old", commentRepository.findByIdAndPostId(ownerPostId, saved.getId()).orElseThrow().getText());
    }

    @Test
    @DisplayName("Deletes only the requested comment")
    void shouldDeleteOnlyRequestedComment() {
        long postId = savedPostId();
        Comment removed = commentRepository.save(new Comment(null, "removed", postId));
        Comment retained = commentRepository.save(new Comment(null, "retained", postId));

        commentRepository.deleteByIdAndPostId(postId, removed.getId());

        assertTrue(commentRepository.findByIdAndPostId(postId, removed.getId()).isEmpty());
        assertTrue(commentRepository.findByIdAndPostId(postId, retained.getId()).isPresent());
    }

    @Test
    @DisplayName("Does not delete a comment for a different post")
    void shouldNotDeleteCommentForDifferentPost() {
        long ownerPostId = savedPostId();
        long otherPostId = savedPostId();
        Comment saved = commentRepository.save(new Comment(null, "comment", ownerPostId));

        commentRepository.deleteByIdAndPostId(otherPostId, saved.getId());

        assertTrue(commentRepository.findByIdAndPostId(ownerPostId, saved.getId()).isPresent());
    }

    private long savedPostId() {
        Post post = new Post();
        post.setTitle("post");
        post.setText("text");
        post.setTags(List.of());
        return postRepository.save(post).getId();
    }
}
